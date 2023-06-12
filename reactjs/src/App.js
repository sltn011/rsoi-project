import './App.css';

import BACKEND_ENABLE from './config.js'

var page = 1
var cars = {'items': []}
var orig_text_display = 'none'
var orig_button_display = 'none'

var user_state = {
  "username": "",
  "user_token": ""
}

const pageDecr = async () => {
  let oldpage = page
  page = page > 1 ? page - 1 : page

  if (page !== oldpage)
  {
    cars = await getCars()
  }
  document.getElementById('page_number').textContent = '  ' + String(page) + '  '
  carsToTable()
}

const pageIncr = async () => {
  page = page + 1
  cars = await getCars()
  if (cars['items'].length === 0)
  {
    page = page - 1
    cars = await getCars()
  }
  document.getElementById('page_number').textContent =  '  ' + String(page) + '  '
  carsToTable()
}

const getCars = async () => {

  if (BACKEND_ENABLE === 0)
  {
    return
  }

  try {
    const response = await fetch('http://localhost:8080/api/v1/cars?page=' + String(page) +'&size=5&showAll=true', {
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

const login_user = async () => {
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

  if (BACKEND_ENABLE === 0)
  {
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

    user_state.username = username
    user_state.user_token = result['token']
    document.getElementById('login_button').disabled = true
    document.getElementById('register_button').disabled = true
    document.getElementById('logout_button').disabled = false
    document.getElementById('login_button').style.display = 'none'
    document.getElementById('register_button').style.display = 'none'
    document.getElementById('username_text').textContent = username
    document.getElementById('username_text').style.display = orig_text_display
    document.getElementById('logout_button').style.display = orig_button_display
    console.log(user_state.user_token)

  } catch (err) {
    console.log(err.message);
  }
}

function validate_email(mail) 
{
 return (/^\w+([.-]?\w+)*@\w+([.-]?\w+)*(.\w{2,3})+$/.test(mail))
}

const register_user = async () => {
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

  if (!validate_email(email))
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

  if (BACKEND_ENABLE === 0)
  {
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

    user_state.username = username
    user_state.user_token = result['token']
    document.getElementById('login_button').disabled = true
    document.getElementById('register_button').disabled = true
    document.getElementById('logout_button').disabled = false
    document.getElementById('login_button').style.display = 'none'
    document.getElementById('register_button').style.display = 'none'
    document.getElementById('username_text').textContent = username
    document.getElementById('username_text').style.display = orig_text_display
    document.getElementById('logout_button').style.display = orig_button_display
    console.log(user_state.user_token)

  } catch (err) {
    console.log(err.message);
  }
}

const logout_user = () => {
  user_state.username = ""
  user_state.user_token = ""
  document.getElementById('login_button').disabled = false
  document.getElementById('register_button').disabled = false
  document.getElementById('logout_button').disabled = true
  document.getElementById('login_button').style.display = orig_button_display
  document.getElementById('register_button').style.display = orig_button_display
  document.getElementById('username_text').style.display = 'none'
  document.getElementById('logout_button').style.display = 'none'
}

const rent_car = async (index) => {
  if (user_state.user_token.length === 0)
  {
    alert('Авторизуйтесь для аренды!')
    return
  }
  
  console.log(cars.items[index])
}

const carsToTable = () => {
  let table = document.getElementById('t_cars')
  table.innerHTML = ""

  let thead = table.insertRow(0)

  let th0 = thead.insertCell()
  th0.className='Cars-table-th-b'
  th0.textContent='Марка'
  let th1 = thead.insertCell()
  th1.className='Cars-table-th-b'
  th1.textContent='Модель'
  let th2 = thead.insertCell()
  th2.className='Cars-table-th-b'
  th2.textContent='Кузов'
  let th3 = thead.insertCell()
  th3.className='Cars-table-th-b'
  th3.textContent='Мощность'
  let th4 = thead.insertCell()
  th4.className='Cars-table-th-b'
  th4.textContent='Цена аренды за день'
  let th5 = thead.insertCell()
  th5.className='Cars-table-th-b'

  for (let i = 0; i < cars['items'].length; i++)
  {
    let row = table.insertRow(i+1)

    let c0 = row.insertCell()
    c0.className='Cars-table-th'
    c0.textContent=cars['items'][i]['brand']
    
    let c1 = row.insertCell()
    c1.className='Cars-table-th'
    c1.textContent=cars['items'][i]['model']
    
    let c2 = row.insertCell()
    c2.className='Cars-table-th'
    c2.textContent=cars['items'][i]['type']
    
    let c3 = row.insertCell()
    c3.className='Cars-table-th'
    c3.textContent=cars['items'][i]['power']
    
    let c4 = row.insertCell()
    c4.className='Cars-table-th'
    c4.textContent=cars['items'][i]['price']
    
    let c5_b = document.createElement("button")
    c5_b.id = "b_" + String(i)
    c5_b.textContent='Арендовать'
    c5_b.className='Cars-table-th'
    c5_b.onclick=function(){rent_car(i)}
    row.append(c5_b)
  }
}

window.onload = async () => {
  cars = await getCars()
  document.getElementById('page_number').textContent = '  ' + String(page) + '  '
  carsToTable()
  orig_text_display = document.getElementById('username_text').style.display
  orig_button_display = document.getElementById('login_button').style.display
  document.getElementById('username_text').style.display = 'none'
  document.getElementById('logout_button').style.display = 'none'
}

function App() {
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

        <button className='Header-button' id={'login_button'} onClick={login_user}>
          Авторизация
        </button>
        <button className='Header-button' id={'register_button'} onClick={register_user}>
          Регистрация
        </button>
        <span id={'username_text'}>
          USERNAME
        </span>
        <button className='Header-button' id={'logout_button'} onClick={logout_user}>
          Выход
        </button>

        <p>
          Автомобили
        </p>
        <button className='Header-button' onClick={pageDecr}>
          ←
        </button>
        <i id={'page_number'}>
        page_number
        </i>
        <button className='Header-button' onClick={pageIncr}>
          →
        </button>
        <br></br>
        <br></br>
        <table id={'t_cars'} className='Cars-table'>
        <tbody id={'cars_table'}>
        </tbody>
        </table>
      </header>

    </div>
  );
}

export default App;
