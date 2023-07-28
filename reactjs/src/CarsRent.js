import React from "react";


class CarsRent extends React.Component {

    constructor(props)
    {
        super(props)

        this.state = {
            page: 1,
            perpage: 5,
            cars: {'items': []},
            user_rents: {'items': []},
            user_rented_cars: {'items': []},
            user_selected_rent_info : {},

            ui_state_cars: 'ui_state_cars',
            ui_state_rent: 'ui_state_rent',
            ui_state_user_rents: 'ui_state_user_rents',
            ui_state_selected_rent_info: 'ui_state_selected_rent_info',
            ui_state_user_rent: 'ui_state_user_rent',
            ui_state_stats: 'ui_state_stats',
            ui_state_loading: 'ui_state_loading',
            ui_state: 'ui_state_cars',

            user_state: {
              "username": "",
              "user_token": "",
            },
        }
    }

    dummy = () => {console.log('Start')}

    awaitSetState = (newState) => new Promise(resolve => this.setState(newState, resolve))

    awaitFunc = async (func, ...args) => {
        let promise = new Promise(resolve => {
            func(...args)
        });
    
        let result = await promise
        return result
    }

    awaitFunc_noret = async (func, ...args) => {
        let promise = new Promise(resolve => {
            func(...args)
        });
    
        await promise
    }

    switchWithDataFetch = async(next_state, func, ...args) => {
      await this.awaitSetState({ui_state: this.state.ui_state_loading})

      await func(...args)

      await this.awaitSetState({ui_state: next_state})
      return
    }

    updateUserRentsData = async() => {
      if (this.state.user_state.user_token.length === 0)
        return 0

      let user_rents = await this.getUserRents()
      this.state.user_rents = user_rents
      let user_rented_cars = await this.getUserRentedCars()
      this.state.user_rented_cars = user_rented_cars

      return 0
    }

    getUserRentInfo = async(rent_uuid) => {
      let url_str = 'http://localhost:8080/api/v1/rental/' + rent_uuid
          
          try {
            const response = await fetch(url_str, {
              method: 'GET',
              headers: {
                Accept: 'application/json',
                Authorization: this.state.user_state.user_token
              },
            });
        
            if (!response.ok) {
              throw new Error(`Error! status: ${response.status}`);
            }
        
            this.state.user_selected_rent_info = await response.json();
            
          } catch (err) {
            alert("Сервис автомобилей не доступен!")
          }
    }

    updateCarsTableData () {
        if (document.getElementById('page_number'))
        {
            document.getElementById('page_number').textContent = '  ' + String(this.state.page) + '  '
        }
    }

    onLoad = async () => {
        await this.awaitSetState({cars: await this.getCars()})
        document.getElementById('page_number').textContent = '  ' + String(this.state.page) + '  '

        if (this.state.user_token && this.state.user_token.length !== 0)
        {
            return
        }
        document.getElementById('username_text').style.display = 'none'
        document.getElementById('logout_button').style.display = 'none'
    }

    componentDidMount = async () => {
        window.requestAnimationFrame(async () => await this.onLoad())
    }

    pageDecr = async () => {
        let oldpage = this.state.page
        let newpage = this.state.page > 1 ? this.state.page - 1 : this.state.page
      
        if (newpage !== oldpage)
        {
            await this.awaitSetState({page: this.state.page > 1 ? this.state.page - 1 : this.state.page})
            await this.awaitSetState({cars: await this.getCars()})
            document.getElementById('page_number').textContent = '  ' + String(this.state.page) + '  '
        }
      }
      
      pageIncr = async () => {
        if (this.state.cars['items'].length === this.state.perpage)
        {
            await this.awaitSetState({page: this.state.page + 1})
            let newcars = await this.getCars()
            await this.awaitSetState({cars: newcars})
            document.getElementById('page_number').textContent =  '  ' + String(this.state.page) + '  '
        }
      }
      
      getCars = async () => {
        console.log('getCars page ' + this.state.page)
        try {
          const response = await fetch('http://localhost:8080/api/v1/cars?page=' + String(this.state.page) +'&size=' + String(this.state.perpage) + '&showAll=true', {
            method: 'GET',
            headers: {
              Accept: 'application/json',
            },
          });
      
          if (!response.ok) {
            throw new Error(`Error! status: ${response.status}`);
          }
      
          const result = await response.json();
      
          console.log(result)
      
          return result
      
        } catch (err) {
          console.log(err.message);
          return {}
        }
      };

      getUserRents = async () => {
        try {
          const response = await fetch('http://localhost:8080/api/v1/rental', {
            method: 'GET',
            headers: {
              Accept: 'application/json',
              Authorization: this.state.user_state.user_token
            },
          });
      
          if (!response.ok) {
            throw new Error(`Error! status: ${response.status}`);
          }
      
          const result = await response.json();
      
          return result
      
        } catch (err) {
          alert("Сервис аренд не доступен!")
        }
      }

      getUserRentedCars = async() => {
        let res = {}
        for (let i = 0; i < this.state.user_rents.length; i++)
        {
          let rent = this.state.user_rents[i]
          let rent_uid = rent['rentalUid']
          let car_uid = rent['car']['carUid']

          let url_str = 'http://localhost:8080/api/v1/car?uid=' + car_uid
          
          try {
            const response = await fetch(url_str, {
              method: 'GET',
              headers: {
                Accept: 'application/json'
              },
            });
        
            if (!response.ok) {
              throw new Error(`Error! status: ${response.status}`);
            }
        
            const car = await response.json();
            res[rent_uid] = car
        
          } catch (err) {
            alert("Сервис автомобилей не доступен!")
          }
        }
        return res
      }
      
      login_user = async () => {
        let username = document.getElementById('nick_input').value
        let password = document.getElementById('password_input').value
      
        console.log(username + " " + password)
      
        if (username.length === 0) {
          alert('Введите имя пользователя!')
          return
        }
        if (password.length === 0) {
          alert('Введите пароль!')
          return
        }
      
        if (username.length < 4) {
          alert('Слишком короткое имя пользователя!')
          return
        }
        if (password.length < 4) {
          alert('Слишком короткий пароль!')
          return
        }
      
        if (username.length > 18) {
          alert('Слишком длинное имя пользователя!')
          return
        }
        if (password.length > 18) {
          alert('Слишком длинное пароль!')
          return
        }
      
        try {
          const response = await fetch('http://localhost:8080/api/v1/login?username=' + username +'&password=' + password, {
            method: 'GET',
            headers: {
              Accept: 'application/json',
            },
          });
      
          if (!response.ok) {
            if (response.status === 400)
            {
              alert('Ошибка авторизации!')
              return
            }
      
            throw new Error(`Error! status: ${response.status} ` + response.body);
          }
      
          const result = await response.json();
      
          await this.awaitSetState({user_state: {username : username}})
          await this.awaitSetState({user_state: {user_token: result['token']}})
          document.getElementById('login_button').disabled = true
          document.getElementById('register_button').disabled = true
          document.getElementById('logout_button').disabled = false
          document.getElementById('login_button').style.display = 'none'
          document.getElementById('register_button').style.display = 'none'
          document.getElementById('username_text').textContent = username
          document.getElementById('username_text').style.display = ''
          document.getElementById('logout_button').style.display = ''
          console.log(this.state.user_state.user_token)
      
        } catch (err) {
          console.log(err.message);
        }

      }
      
      validate_email = (mail) =>
      {
       return (/^\w+([.-]?\w+)*@\w+([.-]?\w+)*(.\w{2,3})+$/.test(mail))
      }
      
      register_user = async () => {
        let email = document.getElementById('email_input').value
        let username = document.getElementById('nick_input').value
        let password = document.getElementById('password_input').value
      
        console.log(email + " " + username + " " + password)
      
        if (email.length === 0) {
          alert('Введите адрес электронной почты пользователя!')
          return
        }
        if (username.length === 0) {
          alert('Введите имя пользователя!')
          return
        }
        if (password.length === 0) {
          alert('Введите пароль!')
          return
        }
      
        if (!this.validate_email(email))
        {
          alert('Некорректный адрес электронной почты!')
          return
        }
        if (username.length < 4) {
          alert('Слишком короткое имя пользователя!')
          return
        }
        if (password.length < 4) {
          alert('Слишком короткий пароль!')
          return
        }
      
        if (email.length > 38)
        {
          alert('Слишком длинный адрес электронной почты!')
          return
        }
        if (username.length > 18) {
          alert('Слишком длинное имя пользователя!')
          return
        }
        if (password.length > 18) {
          alert('Слишком длинное пароль!')
          return
        }
      
        try {
          const response = await fetch('http://localhost:8080/api/v1/register?username=' + username + '&email=' + email +'&password=' + password, {
            method: 'GET',
            headers: {
              Accept: 'application/json',
            },
          });
      
          if (!response.ok) {
            if (response.status === 400)
            {
              alert('Ошибка регистрации!')
              return
            }
      
            throw new Error(`Error! status: ${response.status} ` + response.body);
          }
      
          const result = await response.json();
      
          await this.awaitSetState({user_state: {username : username}})
          await this.awaitSetState({user_state: {user_token: result['token']}})
          document.getElementById('login_button').disabled = true
          document.getElementById('register_button').disabled = true
          document.getElementById('logout_button').disabled = false
          document.getElementById('login_button').style.display = 'none'
          document.getElementById('register_button').style.display = 'none'
          document.getElementById('username_text').textContent = username
          document.getElementById('username_text').style.display = 'inline-block'
          document.getElementById('logout_button').style.display = 'inline-block'
          console.log(this.state.user_state.user_token)
      
        } catch (err) {
          console.log(err.message);
        }
      }
      
      logout_user = async () => {
        await this.awaitSetState({user_state: {username : ""}})
        await this.awaitSetState({user_state: {user_token: ""}})
        document.getElementById('login_button').disabled = false
        document.getElementById('register_button').disabled = false
        document.getElementById('logout_button').disabled = true
        document.getElementById('login_button').style.display = 'inline-block'
        document.getElementById('register_button').style.display = 'inline-block'
        document.getElementById('username_text').style.display = 'none'
        document.getElementById('logout_button').style.display = 'none'
      }
      
      rent_car = async (car) => {
        if (this.state.user_state.user_token.length === 0)
        {
          alert('Авторизуйтесь для аренды!')
          return
        }
      
        console.log(car)
      }

      finish_rent = (rent_id) => {
        console.log('Finish rent ' + rent_id)
      }

      cancel_rent = (rent_id) => {
        console.log('Cancel rent ' + rent_id)
      }

      carsTableHeader = () => {
        let header_names = ['Марка', "Модель", "Кузов", "Мощность", "Цена аренды за день", ""]

        const cars_table_hcontent = () => {
          return (
            header_names.map((data) => {
              return <th key={data}>{data}</th>
            })
          )
        }

        return (
          <thead>
            <tr>
              {cars_table_hcontent()}
            </tr>
          </thead>
        )
      }

      carsTableBody = () => {
        const cars_table_content = () => {
          return this.state.cars.items.map((c) => {
            return(
              <tr key={'b_' + c['carUid']}>
                <td>{c['brand']}</td>
                <td>{c['model']}</td>
                <td>{c['type']}</td>
                <td>{c['power']}</td>
                <td>{c['price']}</td>
                <td><button id={'b_' + c['carUid']} onClick={() => this.rent_car(c)}>Арендовать</button></td>
              </tr>
            )
          })
        }

        return (
          <tbody>
            {cars_table_content()}
          </tbody>
        )

      }
      
      carsToTable = () => {
        if (this.state.cars.items.length === 0)
        {
          return <h4>cars table</h4>
        }

        return (
          <table id='cars_table' className="App-table">
            {this.carsTableHeader()}
            {this.carsTableBody()}
          </table>
        )
      }
     
      getCarsUI = () => {
        return (
          <React.Fragment>
              <p>
                Автомобили
              </p>
              <button className='Header-button' onClick={this.pageDecr}>
                ←
              </button>
              <i id={'page_number'}>
              {this.state.page}
              </i>
              <button className='Header-button' onClick={this.pageIncr}>
                →
              </button>
              <br></br>
              <br></br>
              {this.carsToTable()}
          </React.Fragment>
        )
      }

      userRentsTableHeader = () => {
        let header_names = ["Информация", "Статус", "Начало", "Конец", "Завершить"]

        const user_rents_table_hcontent = () => {
          return (
            header_names.map((data) => {
              return <th key={data}>{data}</th>
            })
          )
        }

        return (
          <thead>
            <tr>
              {user_rents_table_hcontent()}
            </tr>
          </thead>
        )
      }

      userRentsTableBody = () => {
        const user_rents_table_content = () => {
          return this.state.user_rents.map((c) => {
            let rent_status = c['status']
            let descr = rent_status === 'IN_PROGRESS' ? 'Текущая' : (rent_status === 'FINISHED' ? 'Истекла' : 'Отменена')
            let b_can_cancel = Date.parse(c['dateFrom']) > Date.now()
            let b_finished = rent_status !== 'IN_PROGRESS'

            let info = (
              <button id={'i_' + c['rentalUid']} onClick={async () => { 
                await this.switchWithDataFetch(this.state.ui_state_selected_rent_info, this.getUserRentInfo, c['rentalUid'])
              }}>Информация</button>
            )

            let action = <h6> </h6>
            if (!b_finished)
            {
              if (b_can_cancel)
              {
                action = <button id={'r_' + c['rentalUid']} onClick={() => this.cancel_rent(c['rentalUid'])}>Отмена</button>
              }
              else
              {
                action = <button id={'r_' + c['rentalUid']} onClick={() => this.finish_rent_rent(c['rentalUid'])}>Завершить</button>
              }
            }

            return(
              <tr key={'r_' + c['rentalUid']}>
                <td>{info}</td>
                <td>{descr}</td>
                <td>{c['dateFrom']}</td>
                <td>{c['dateTo']}</td>
                <td>{action}</td>
              </tr>
            )
          })
        }

        return (
          <tbody>
            {user_rents_table_content()}
          </tbody>
        )

      }

      userRentsTable = () => {
        if (this.state.user_state.user_token.length === 0)
        {
          return <h3>Вы не авторизованы в системе!</h3>
        }

        return (
          <table id='user_rents_table' className="App-table">
            {this.userRentsTableHeader()}
            {this.userRentsTableBody()}
          </table>
        )
      }
      
      getUserRentsUI = () => {
        return (
          <React.Fragment>
            <p>Ваши аренды</p>
            <br></br>
            <br></br>
            {this.userRentsTable()}
          </React.Fragment>
        )
      }

      getUserSelectedRentInfoUI = () => {
        if (this.state.user_selected_rent_info === {})
        {
          return (
          <React.Fragment>
            <p>Ошибка</p>
          </React.Fragment>
          )
        }
        
        let rent = this.state.user_selected_rent_info
        return (
          <React.Fragment>
            <p>Информация об аренде</p>
            <br></br>
            <div>{`Автомобиль: ${rent['car']['brand']} ${rent['car']['model']}`}</div>
            <br></br>
            <div>{`Срок аренды: ${rent['dateFrom']} - ${rent['dateTo']}`}</div>
            <div>{`Статус: ${rent['status'] === 'IN_PROGRESS' ? 'Текущая' : (rent['status'] === 'FINISHED' ? 'Истекла' : 'Отменена')}`}</div>
            <br></br>
            <div>{`Оплата: Цена ${rent['payment']['price']}руб`}</div>
            <div>{`Статус: ${rent['payment']['status'] === 'PAID' ? 'Оплачено' : 'Отменено'}`}</div>
          </React.Fragment>
        )
      }

      getLoadingUI = () => {
        return (
          <React.Fragment>
            <p>Обработка запроса</p>
          </React.Fragment>
        )
      }
      
      getCurrentUI () {
        switch (this.state.ui_state)
        {
          case this.state.ui_state_loading:
            return this.getLoadingUI()
          case this.state.ui_state_cars:
            this.updateCarsTableData()
            return this.getCarsUI()
          case this.state.ui_state_user_rents:
            return this.getUserRentsUI()
          case this.state.ui_state_selected_rent_info:
            return this.getUserSelectedRentInfoUI()
          default:
            return (<h1>ERROR</h1>)
        }
      }

    render() {
        return (
        <div className="App">
        <header className="App-header">
        <p>
          Аренда автомобилей
        </p>

        <form id={"auth_form"}>
        <input className='Text_input' type={"text"} id={"email_input"} placeholder='email'>
        </input>
        <input className='Text_input' type={"text"} id={"nick_input"} placeholder='Имя пользователя'>
        </input>
        <input className='Text_input' type={"password"} id={"password_input"} placeholder='Пароль'>
        </input>
        </form>

        <button className='UISelector' id={'login_button'} onClick={this.login_user}>
          Авторизация
        </button>
        <button className='UISelector' id={'register_button'} onClick={this.register_user}>
          Регистрация
        </button>
        <span id={'username_text'}>
          USERNAME
        </span>
        <button className='UISelector' id={'logout_button'} onClick={this.logout_user}>
          Выход
        </button>

        <br/>

        <button className='UISelector' onClick={async () => await this.awaitSetState({ui_state: this.state.ui_state_cars, page: 1})}>
          Автомобили
        </button>
        <button className='UISelector' onClick={async () => await this.switchWithDataFetch(this.state.ui_state_user_rents, this.updateUserRentsData)}>
          Ваши аренды
        </button>

        {this.getCurrentUI()}
        
        </header>

        </div>
        )
    }

}

export default CarsRent;