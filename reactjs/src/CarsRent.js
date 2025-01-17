import React from "react";


class CarsRent extends React.Component {

    constructor(props)
    {
        super(props)

        this.state = {
            page: 1,
            perpage: 5,
            cars: undefined,
            user_rents: undefined,
            user_rented_cars: undefined,
            user_selected_rent_info : undefined,
            car_for_rent : undefined,
            services_stats : undefined,

            ui_state_cars: 'ui_state_cars',
            ui_state_rent: 'ui_state_rent',
            ui_state_user_rents: 'ui_state_user_rents',
            ui_state_selected_rent_info: 'ui_state_selected_rent_info',
            ui_state_create_rent: 'ui_state_create_rent',
            ui_state_stats: 'ui_state_stats',
            ui_state_error: 'ui_state_error',
            ui_state_loading: 'ui_state_loading',
            ui_state: 'ui_state_cars',

            user_state: {
              "username": "",
              "user_token": "",
              "user_role": "USER"
            },

            error_message: ""
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

      try{
        await func(...args)
      }
      catch (e)
      {
        console.log(e.message)
        this.setState({error_message: e.message, ui_state: this.state.ui_state_error})
        this.forceUpdate()
        return
      }

      await this.awaitSetState({ui_state: next_state})
      return
    }

    updateUserRentsData = async() => {
      if (this.state.user_state.user_token.length === 0)
        return 0

      let user_rents = await this.getUserRents()
      this.state.user_rents = user_rents

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
              throw new Error(`Не удалось получить список аренд!`);
            }
        
            this.state.user_selected_rent_info = await response.json();
            
          } catch (err) {
            alert("Не удалось получить список аренд!")
          }
    }

    finish_rent_action = async(rent_uuid) => {
      let url_str = 'http://localhost:8080/api/v1/rental/' + rent_uuid + '/finish'
          
          try {
            const response = await fetch(url_str, {
              method: 'POST',
              headers: {
                Accept: 'application/json',
                Authorization: this.state.user_state.user_token
              },
            });
        
            if (response.status !== 204) {
              throw new Error(`Не удалось завершить аренду`);
            }
            
          } catch (err) {
            throw new Error("Не удалось завершить аренду!")
          }

        await this.updateUserRentsData()
    }

    cancel_rent_action = async(rent_uuid) => {
      let url_str = 'http://localhost:8080/api/v1/rental/' + rent_uuid
          
          try {
            const response = await fetch(url_str, {
              method: 'DELETE',
              headers: {
                Accept: 'application/json',
                Authorization: this.state.user_state.user_token
              },
            });
        
            if (response.status !== 204) {
              throw new Error(`Не удалось отменить аренду`);
            }
            
          } catch (err) {
            throw new Error("Не удалось отменить аренду!")
          }

      await this.updateUserRentsData()
    }

    updateCarsTableData () {
        if (document.getElementById('page_number'))
        {
            document.getElementById('page_number').textContent = '  ' + String(this.state.page) + '  '
        }
    }

    onLoad = async () => {
        document.getElementById('page_number').textContent = '  ' + String(this.state.page) + '  '

        if (this.state.user_token && this.state.user_token.length !== 0)
        {
            return
        }
        document.getElementById('username_text').style.display = 'none'
        document.getElementById('logout_button').style.display = 'none'
        
        try {
          let loadfunc = async () => {
            await this.switchWithDataFetch(this.state.ui_state_cars, async() => {
              this.state.page = 1
              this.state.cars = await this.getCars()
            })
          }

          await loadfunc()
        }
        catch(e)
        {
          console.log(e.message)
          this.setState({error_message: e.message, ui_state: this.state.ui_state_error})
          this.forceUpdate()
          return
        }
    }

    componentDidMount = async () => {
        window.requestAnimationFrame(async () => await this.onLoad())
    }

    getStats = async() => {
      try {
        const response = await fetch('http://localhost:8080/api/v1/avgTime', {
          method: 'GET',
          headers: {
            Accept: 'application/json',
            Authorization: this.state.user_state.user_token
          },
        });
    
        if (!response.ok) {
          throw new Error('Ошибка запроса статистики!')
        }
    
        const result = await response.json();
        console.log(result)

        this.state.services_stats = result
      }
      catch(e)
      {
        console.log(e)
        throw new Error('Ошибка запроса статистики!')
      }
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
          const response = await fetch('http://localhost:8080/api/v1/cars?page=' + String(this.state.page) +'&size=' + String(this.state.perpage) + '&showAll=false', {
            method: 'GET',
            headers: {
              Accept: 'application/json',
            },
          });
      
          if (!response.ok) {
            throw new Error(`Не удалось получить список автомобилей`);
          }
      
          const result = await response.json();
      
          console.log(result)
      
          return result
      
        } catch (err) {
          throw new Error(`Не удалось получить список автомобилей`);
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
            throw new Error("Не удалось получить список аренд!")
          }
      
          const result = await response.json();
      
          return result
      
        } catch (err) {
          throw new Error("Не удалось получить список аренд!")
        }
      }

      get_user_role = async (token) => {
        try {
          const response = await fetch('http://localhost:8080/api/v1/role', {
            method: 'GET',
            headers: {
              Accept: 'application/json',
              Authorization: token
            },
          });
      
          if (!response.ok) {
            throw new Error('Ошибка авторизации!')
          }
      
          const result = await response.json();
          console.log(result)
          let user_role = result['role']
          console.log('user_role ' + user_role)

          if (user_role === 'INVALID')
          {
            throw new Error('Ошибка авторизации!')
          }

          this.state.user_state.user_role = user_role
        }
        catch(e)
        {
          console.log(e)
          throw new Error('Ошибка авторизации!')
        }
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
      
            throw new Error(`Ошибка авторизации!`);
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

          await this.get_user_role(result['token'])
      
        } catch (err) {
          throw new Error('Ошибка авторизации!')
        }

        await this.switchWithDataFetch(this.state.ui_state_cars, async() => {})
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
        await this.switchWithDataFetch(this.state.ui_state_cars, async() => {})
      }

      getDateStr = (date) => {
        let day = String(date.getDate());
        let month = String(date.getMonth() + 1);
        if (month.length === 1)
        {
          month = "0" + month
        }
        let year = String(date.getFullYear());
        return `${year}-${month}-${day}`
      }
      
      logout_user = async () => {
        let action = async() => {
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

        await this.switchWithDataFetch(this.state.ui_state_cars, action)
      }
      
      rent_car = async (car) => {
        if (this.state.user_state.user_token.length === 0)
        {
          alert('Авторизуйтесь для аренды!')
          return
        }
      
        console.log(car)

        await this.awaitSetState({ui_state: this.state.ui_state_create_rent, car_for_rent: car})
      }

      getRentCost = (start, finish) => {
        let per_day = this.state.car_for_rent['price']

        let diff = Date.parse(finish) - Date.parse(start)
        let days = Math.floor(diff / (1000 * 3600 * 24))

        if (days <= 0)
        {
          alert('Введена некорректная дата аренды!')
          return 0
        }

        if (days > 30)
        {
          alert('Нельзя брать автомобиль на такой долгий срок!')
          return 0
        }

        let price = per_day * days

        if (!price)
        {
          return 0
        }

        let cost_disp = document.getElementById("rent_cost")
        if (cost_disp)
        {
          cost_disp.innerHTML = price
        }
        return price
      }

      tryRenting = async(car, start, finish) => {
        let price = this.getRentCost(start, finish)
        if (!price || price <= 0)
        {
          alert('Ошибка при расчете стоимости аренды!')
          throw new Error('Неверные данные введены при создании аренды!')
        }

        let url_str = 'http://localhost:8080/api/v1/rental/'

        try {
          const response = await fetch(url_str, {
            method: 'POST',
            headers: {
              Accept: 'application/json',
              'Content-Type': 'application/json',
              Authorization: this.state.user_state.user_token
            },
            body: JSON.stringify({carUid: car['carUid'], dateFrom: start, dateTo: finish})
          });
      
          if (!response.ok) {
            throw new Error("Не удалось создать аренду!")
          }

          const result = await response.json();
          let rentalUid = result['rentalUid']

          await this.getUserRentInfo(rentalUid)
          
        } catch (err) {
          throw new Error("Не удалось создать аренду!")
        }
      }

      getCarRentUI = () => {
        let car = this.state.car_for_rent

        let today_date = this.getDateStr(new Date())

        return (
          <React.Fragment>
            <p>
              Аренда
            </p>
            <div>{`Автомобиль: ${car['brand']} ${car['model']} ${car['type']}`}</div>
            <div>{`Цена за день аренды: ${car['price']}руб`}</div>
            <br></br>
            <label for="rent-start">Начало аренды:</label>
            <br></br>
            <input className="UIDate" type="date" id="rent-start" name="rent-start"
              min={today_date}>
            </input>
            <br></br>
            <label for="rent-end">Конец аренды:</label>
            <br></br>
            <input className="UIDate" type="date" id="rent-end" name="rent-end"
              min={today_date}>
            </input>
            <br></br>
            <br></br>
            <button className='UIButton' onClick={() => this.getRentCost(document.getElementById('rent-start').value, document.getElementById('rent-end').value)}>
              Рассчитать стоимость
            </button>
            <div>{'Стоимость аренды: '}</div>
            <div id={'rent_cost'}>0</div>
            <br></br>
            <br></br>
            <button className='UIButton' onClick={async() => 
              {
                this.switchWithDataFetch(
                  this.state.ui_state_selected_rent_info,
                  this.tryRenting,
                  car,
                  document.getElementById('rent-start').value,
                  document.getElementById('rent-end').value
                )
              }
            }>Арендовать</button>
        </React.Fragment>
        )
      }

      finish_rent = async (rent_id) => {
        console.log('Finish rent ' + rent_id)
        await this.switchWithDataFetch(this.state.ui_state_user_rents, this.finish_rent_action, rent_id)
      }

      cancel_rent = async (rent_id) => {
        console.log('Cancel rent ' + rent_id)
        await this.switchWithDataFetch(this.state.ui_state_user_rents, this.cancel_rent_action, rent_id)
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
        if (!this.state.cars || this.state.cars === undefined)
        {
          return
        }

        const cars_table_content = () => {
          return this.state.cars.items.map((c) => {
            return(
              <tr key={'b_' + c['carUid']}>
                <td>{c['brand']}</td>
                <td>{c['model']}</td>
                <td>{c['type']}</td>
                <td>{c['power']}</td>
                <td>{c['price']}</td>
                <td><button className='UIButton' id={'b_' + c['carUid']} onClick={async () => await this.rent_car(c)}>Арендовать</button></td>
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
        if (!this.state.cars || this.state.cars === undefined || this.state.cars.items.length === 0)
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
        if (!this.state.user_rents || this.state.user_rents === undefined)
        {
          return
        }

        const user_rents_table_content = () => {
          return this.state.user_rents.map((c) => {
            let rent_status = c['status']
            let descr = rent_status === 'IN_PROGRESS' ? 'Текущая' : (rent_status === 'FINISHED' ? 'Завершена' : 'Отменена')
            let b_can_cancel = Date.parse(c['dateFrom']) > Date.now()
            let b_finished = rent_status !== 'IN_PROGRESS'

            let info = (
              <button className='UIButton' id={'i_' + c['rentalUid']} onClick={async () => { 
                await this.switchWithDataFetch(this.state.ui_state_selected_rent_info, this.getUserRentInfo, c['rentalUid'])
              }}>Информация</button>
            )

            let action = <h6> </h6>
            if (!b_finished)
            {
              if (b_can_cancel)
              {
                action = <button className='UIButton' id={'r_' + c['rentalUid']} onClick={async () => await this.cancel_rent(c['rentalUid'])}>Отмена</button>
              }
              else
              {
                action = <button className='UIButton' id={'r_' + c['rentalUid']} onClick={async () => await this.finish_rent(c['rentalUid'])}>Завершить</button>
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
            <div>{`Статус: ${rent['status'] === 'IN_PROGRESS' ? 'Текущая' : (rent['status'] === 'FINISHED' ? 'Завершена' : 'Отменена')}`}</div>
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

      getStatsTableHeader = () => {
        let header_names = ["Сервис", "Среднее время обработки запроса"]

        const stats_table_hcontent = () => {
          return (
            header_names.map((data) => {
              return <th key={data}>{data}</th>
            })
          )
        }

        return (
          <thead>
            <tr>
              {stats_table_hcontent()}
            </tr>
          </thead>
        )
      }

      getStatsTableBody = () => {
        const avg = this.state.services_stats

        return (
          <React.Fragment>
            <tr><td>Gateway сервис</td><td>{`${avg['GatewayAvgTime'] / 1000}мс`}</td></tr>
            <tr><td>Сервис аккаунтов</td><td>{`${avg['AccAvgTime'] / 1000}мс`}</td></tr>
            <tr><td>Сервис автомобилей</td><td>{`${avg['CarsAvgTime'] / 1000}мс`}</td></tr>
            <tr><td>Сервис аренд</td><td>{`${avg['RentalAvgTime'] / 1000}мс`}</td></tr>
            <tr><td>Сервис платежей</td><td>{`${avg['PaymentAvgTime'] / 1000}мс`}</td></tr>
          </React.Fragment>
        )
      }

      getStatsUI = () => {
        return (
          <React.Fragment>
            <p>Статистика работы сервисов</p>
            {this.getStatsTableHeader()}
            {this.getStatsTableBody()}
          </React.Fragment>
        )
      }

      getErrorUI = () => {
        return (
          <React.Fragment>
            <p>{`Ошибка: ${this.state.error_message}`}</p>
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
          case this.state.ui_state_create_rent:
            return this.getCarRentUI()
          case this.state.ui_state_stats:
            return this.getStatsUI()
          case this.state.ui_state_error:
            return this.getErrorUI()
          default:
            return (<h1>ERROR</h1>)
        }
      }

    render() {

        let admin_control = <div></div>
        if (this.state.user_state.user_role === 'ADMIN')
        {
          admin_control = (
            <button className='UIButton' onClick={async () => await this.switchWithDataFetch(this.state.ui_state_stats, this.getStats)}>
            Панель администратора
            </button>
          )
        }

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
  
          <button className='UIButton' id={'login_button'} onClick={async() => await this.switchWithDataFetch(this.state.ui_state_cars, this.login_user)}>
            Авторизация
          </button>
          <button className='UIButton' id={'register_button'} onClick={async() => await this.switchWithDataFetch(this.state.ui_state_cars, this.register_user)}>
            Регистрация
          </button>
          <span id={'username_text'}>
            USERNAME
          </span>
          <button className='UIButton' id={'logout_button'} onClick={async() => await this.logout_user()}>
            Выход
          </button>
  
          <br/>
  
          {admin_control}
  
          <br/>
  
          <button className='UIButton' onClick={async () => {
            await this.switchWithDataFetch(this.state.ui_state_cars, async() => {
              this.state.page = 1
              this.state.cars = await this.getCars()
            })
          }}>
            Автомобили
          </button>
          <button className='UIButton' onClick={async () => await this.switchWithDataFetch(this.state.ui_state_user_rents, this.updateUserRentsData)}>
            Ваши аренды
          </button>
  
          {this.getCurrentUI()}
          
          </header>
  
          </div>
          )
    }

}

export default CarsRent;