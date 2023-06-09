import './App.css';

import BACKEND_ENABLE from './config.js'

var page = 1
var cars = {'items': []}

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

const carsToTable = () => {
  var res = ""
  for (let i = 0; i < cars['items'].length; i++)
  {
      res += "<tr>"
      res += "<th className='Cars-table-th'>" + cars['items'][i]['brand'] + "</th>"
      res += "<th className='Cars-table-th'>" + cars['items'][i]['model'] + "</th>"
      res += "<th className='Cars-table-th'>" + cars['items'][i]['type'] + "</th>"
      res += "<th className='Cars-table-th'>" + cars['items'][i]['power'] + "</th>"
      res += "<th className='Cars-table-th'>" + cars['items'][i]['price'] + "</th>"
      res += "</tr>"
  }
  document.getElementById('cars_table').innerHTML = res;
}

window.onload = async () => {
  cars = await getCars()
  document.getElementById('page_number').textContent = '  ' + String(page) + '  '
  carsToTable()
}

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <p>
          Аренда автомобилей
        </p>
        <button className='Header-button' onClick={getCars}>
          Логин
        </button>
        <button className='Header-button'>
          Регистрация
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
        <table className='Cars-table'>
        <thead><tr>
        <th className='Cars-table-th'> Марка </th>
        <th className='Cars-table-th'> Модель </th>
        <th className='Cars-table-th'> Кузов </th>
        <th className='Cars-table-th'> Мощность </th>
        <th className='Cars-table-th'> Цена аренды за день </th>
        </tr></thead>
        <tbody id={'cars_table'}>
        </tbody>
        </table>
      </header>

    </div>
  );
}

export default App;
