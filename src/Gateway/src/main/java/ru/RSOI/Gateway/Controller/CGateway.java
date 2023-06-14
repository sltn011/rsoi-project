package ru.RSOI.Gateway.Controller;

import Utils.AvgTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import ru.RSOI.Gateway.Error.EBadRequestError;
import ru.RSOI.Gateway.Error.ENotFoundError;
import ru.RSOI.Gateway.Error.EUnauthorized;
import ru.RSOI.Gateway.Model.*;

import java.security.interfaces.RSAPublicKey;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@CrossOrigin
@RequestMapping("/api/v1")
public class CGateway {

    public static final String CarsService    = "http://localhost:8070/api/v1/sys/cars";
    public static final String RentService    = "http://localhost:8060/api/v1/sys/rental";
    public static final String PaymentService = "http://localhost:8050/api/v1/sys/payment";
    public static final String AccService     = "http://localhost:8010/api/v1/sys/acc";
    public static final String StatsService   = "http://localhost:8100/api/v1/sys/stats/avgTime";

    public static final String AuthService    = "http://localhost:9889/auth";

    private AvgTime avgTime;

    public CGateway()
    {
        this.avgTime = new AvgTime();
    }

    @GetMapping("/hello")
    @CrossOrigin(origins = "*")
    public String healthcheck()
    {
        return "Hello from gateway!";
    }

    @GetMapping("/register")
    public String register(@RequestParam String username, @RequestParam String email, @RequestParam String password)
    {
        AvgTime avg = new AvgTime();
        avg.begin();

        if (checkUserExists(username))
        {
            throw new EBadRequestError("Username taken!", new ArrayList<>());
        }

        addUser(username, email, password);

        String token = getUserToken(username, email, new String("USER"));
        if (token.length() == 0)
        {
            throw new EBadRequestError("Error registering!", new ArrayList<>());
        }

        JSONObject res = new JSONObject();
        res.put("token", token);

        avg.end();
        avgTime.add(avg.get());

        return res.toString();
    }

    @GetMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password)
    {
        AvgTime avg = new AvgTime();
        avg.begin();

        if (!checkUserExists(username))
        {
            throw new EBadRequestError("User not found!", new ArrayList<>());
        }

        JSONObject userInfo = getUserInfo(username);

        if (!password.equals(userInfo.getString("password")))
        {
            throw new EBadRequestError("User not found!", new ArrayList<>());
        }

        String token = getUserToken(userInfo.getString("username"), userInfo.getString("email"), userInfo.getString("role"));
        if (token.length() == 0)
        {
            throw new EBadRequestError("Error registering!", new ArrayList<>());
        }

        JSONObject res = new JSONObject();
        res.put("token", token);

        avg.end();
        avgTime.add(avg.get());

        return res.toString();
    }

    @GetMapping("/cars")
    public MCarsPage getAvailableCars(@RequestParam int page, @RequestParam int size,
                                      @RequestParam(defaultValue = "false") boolean showAll)
    {
        AvgTime avg = new AvgTime();
        avg.begin();

        String url = UriComponentsBuilder.fromHttpUrl(CarsService)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("showAll", showAll)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            avg.end();
            avgTime.add(avg.get());
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            avg.end();
            avgTime.add(avg.get());
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            avg.end();
            avgTime.add(avg.get());
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            avg.end();
            avgTime.add(avg.get());
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            avg.end();
            avgTime.add(avg.get());
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }

        JSONObject obj = new JSONObject(response.getBody());

        int totalElements = obj.getInt("totalElements");

        JSONArray jsonCars = obj.getJSONArray("content");
        int numCars = jsonCars.length();
        ArrayList<MCarInfo> carsInfo = new ArrayList<>(numCars);
        for (int i = 0; i < numCars; ++i)
        {
            JSONObject jsonCar = jsonCars.getJSONObject(i);
            MCarInfo carInfo = parseCarInfo(jsonCar);
            carsInfo.add(carInfo);
        }

        MCarsPage carsPage = new MCarsPage(page, size, totalElements, carsInfo);
        avg.end();
        avgTime.add(avg.get());
        return carsPage;
    }

    @GetMapping("/rental")
    public List<MRentInfo> getAllUserRents(@RequestHeader(value = "Authorization", required = false) String access_token)
    {
        AvgTime avg = new AvgTime();
        avg.begin();

        System.out.println(access_token);
        access_token = fixToken(access_token);
        System.out.println(access_token);

        if (!IsValidToken(access_token))
        {
            avg.end();
            avgTime.add(avg.get());
            throw new EUnauthorized("Not authorized!");
        }
        String username = getUsername(access_token);
        List res = getAllUserRentsList(username);
        avg.end();
        avgTime.add(avg.get());
        return res;
    }

    @PostMapping("/rental")
    public MRentSuccess tryRenting(@RequestHeader(value = "Authorization", required = false) String access_token,
                                   @RequestBody Map<String, String> values)
    {
        AvgTime avg = new AvgTime();
        avg.begin();

        System.out.println(access_token);
        access_token = fixToken(access_token);
        System.out.println(access_token);

        if (!IsValidToken(access_token))
        {
            avg.end();
            avgTime.add(avg.get());
            throw new EUnauthorized("Not authorized!");
        }
        String username = getUsername(access_token);

        if (!values.containsKey("carUid") || !values.containsKey("dateFrom") || !values.containsKey("dateTo"))
        {
            avg.end();
            avgTime.add(avg.get());
            throw new EBadRequestError("Not all variables in request!", new ArrayList<>());
        }

        String carUid = values.get("carUid");
        String dateFrom = values.get("dateFrom");
        String dateTo = values.get("dateTo");
        MCarInfo car = requestAvailableCar(carUid);

        Date dateFromVal = Date.valueOf(dateFrom);
        Date dateToVal = Date.valueOf(dateTo);
        long diffInMillies = Math.abs(dateToVal.getTime() - dateFromVal.getTime());
        int diff = (int)TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        MRentPaymentInfo paymentInfo = createPayment(diff * car.price);
        MRentInfo rentInfo = createRent(username, carUid, paymentInfo.paymentUid.toString(), dateFrom, dateTo);

        avg.end();
        avgTime.add(avg.get());
        return new MRentSuccess(rentInfo.rentalUid, rentInfo.status, rentInfo.car.carUid,
                rentInfo.dateFrom, rentInfo.dateTo, paymentInfo);
    }

    @GetMapping("/rental/{rentalUid}")
    public MRentInfo getUserRent(@RequestHeader(value = "Authorization", required = false) String access_token,
                                 @PathVariable String rentalUid)
    {
        AvgTime avg = new AvgTime();
        avg.begin();

        System.out.println(access_token);
        access_token = fixToken(access_token);
        System.out.println(access_token);

        if (!IsValidToken(access_token))
        {
            avg.end();
            avgTime.add(avg.get());
            throw new EUnauthorized("Not authorized!");
        }
        String username = getUsername(access_token);

        MRentInfo res = getUserRentByUid(username, rentalUid);
        avg.end();
        avgTime.add(avg.get());
        return res;
    }

    @DeleteMapping("/rental/{rentalUid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void cancelUserRent(@RequestHeader(value = "Authorization", required = false) String access_token,
                               @PathVariable String rentalUid)

    {
        AvgTime avg = new AvgTime();
        avg.begin();

        System.out.println(access_token);
        access_token = fixToken(access_token);
        System.out.println(access_token);

        if (!IsValidToken(access_token))
        {
            throw new EUnauthorized("Not authorized!");
        }
        String username = getUsername(access_token);
        MRentInfo rentInfo = getUserRentByUid(username, rentalUid);
        if (rentInfo.status.equals("IN_PROGRESS"))
        {
            setCarAvailable(rentInfo.car.carUid.toString(), true);
            cancelRent(username, rentalUid);
            cancelPayment(rentInfo.payment.paymentUid.toString());
        }
        avg.end();
        avgTime.add(avg.get());
    }

    @PostMapping("/rental/{rentalUid}/finish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finishUserRent(@RequestHeader(value = "Authorization", required = false) String access_token,
                               @PathVariable String rentalUid)
    {
        AvgTime avg = new AvgTime();
        avg.begin();

        System.out.println(access_token);
        access_token = fixToken(access_token);
        System.out.println(access_token);

        if (!IsValidToken(access_token))
        {
            throw new EUnauthorized("Not authorized!");
        }
        String username = getUsername(access_token);
        MRentInfo rentInfo = getUserRentByUid(username, rentalUid);
        if (rentInfo.status.equals("IN_PROGRESS"))
        {
            setCarAvailable(rentInfo.car.carUid.toString(), true);
            finishRent(username, rentalUid);
        }
        avg.end();
        avgTime.add(avg.get());
    }

    @GetMapping("/stats")
    public String getAvgTime(@RequestHeader(value = "Authorization", required = false) String access_token)
    {
        AvgTime avg = new AvgTime();
        avg.begin();

        System.out.println(access_token);
        access_token = fixToken(access_token);
        System.out.println(access_token);

        String role = getRole(access_token);
        if (role != "ADMIN")
        {
            avg.end();
            avgTime.add(avg.get());
            return new String();
        }

        String url = UriComponentsBuilder.fromHttpUrl(StatsService)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            return new String();
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            return new String();
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            return new String();
        }

        avg.end();
        avgTime.add(avg.get());
        return response.getBody();
    }

    private MRentCarInfo getRentCarInfo(String carUid)
    {
        String url = UriComponentsBuilder.fromHttpUrl(CarsService + "/" + carUid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }


        JSONObject obj = new JSONObject(response.getBody());

        try
        {
            UUID carUidVal = UUID.fromString(carUid);
            String brand = obj.getString("v2_brand");
            String model = obj.getString("v3_model");
            String registrationNumber = obj.getString("v4_registration_number");
            return new MRentCarInfo(carUidVal, brand, model, registrationNumber);
        }
        catch (IllegalArgumentException e)
        {
            throw new EBadRequestError("Invalid values passed", new ArrayList<>());
        }

    }

    private MRentPaymentInfo getRentPaymentInfo(String paymentUid)
    {
        String url = UriComponentsBuilder.fromHttpUrl(PaymentService + "/" + paymentUid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }

        JSONObject obj = new JSONObject(response.getBody());

        try
        {
            UUID paymentUidVal = UUID.fromString(paymentUid);
            String status = obj.getString("v2_status");
            int price = obj.getInt("v3_price");

            return new MRentPaymentInfo(paymentUidVal, status, price);
        }
        catch (IllegalArgumentException e)
        {
            throw new EBadRequestError("Invalid values passed", new ArrayList<>());
        }
    }

    private MRentInfo getRentInfoFromJSON(JSONObject obj)
    {
        UUID rentalUid = UUID.fromString(obj.getString("v1_rental_uid"));
        String status = obj.getString("v7_status");
        String dateFrom = obj.getString("v5_date_from");
        String dateTo = obj.getString("v6_date_to");

        int ind1 = dateFrom.indexOf('T');
        if (ind1 != -1)
        {
            dateFrom = dateFrom.substring(0, ind1);
        }
        int ind2 = dateTo.indexOf('T');
        if (ind2 != -1)
        {
            dateTo = dateTo.substring(0, ind2);
        }

        MRentCarInfo rentCarInfo = getRentCarInfo(obj.getString("v4_car_uid"));
        MRentPaymentInfo rentPaymentInfo = getRentPaymentInfo(obj.getString("v3_payment_uid"));

        return new MRentInfo(rentalUid, status, dateFrom, dateTo, rentCarInfo, rentPaymentInfo);
    }

    private MCarInfo parseCarInfo(JSONObject obj)
    {
        UUID carUid = UUID.fromString(obj.getString("v1_car_uid"));
        String brand = obj.getString("v2_brand");
        String model = obj.getString("v3_model");
        String registrationNumber = obj.getString("v4_registration_number");
        int power = obj.getInt("v5_power");
        String type = obj.getString("v7_type");
        int price = obj.getInt("v6_price");
        boolean available = obj.getBoolean("v8_availability");

        return new MCarInfo(carUid, brand, model, registrationNumber, power, type, price, available);
    }
    
    private MCarInfo requestAvailableCar(String carUid)
    {
        String url = UriComponentsBuilder.fromHttpUrl(CarsService + "/request/" + carUid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }

        JSONObject obj = new JSONObject(response.getBody());
        return parseCarInfo(obj);
    }

    List<MRentInfo> getAllUserRentsList(String username)
    {
        String url = UriComponentsBuilder.fromHttpUrl(RentService)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-User-Name", username);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }

        JSONArray rents = new JSONArray(response.getBody());
        int numRents = rents.length();
        ArrayList<MRentInfo> rentsInfo = new ArrayList<>(numRents);
        for (int i = 0; i < numRents; ++i)
        {
            JSONObject rentInfo = rents.getJSONObject(i);
            rentsInfo.add(getRentInfoFromJSON(rentInfo));
        }

        return rentsInfo;
    }

    MRentInfo getUserRentByUid(String username, String rentalUid)
    {
        List<MRentInfo> allUserRents = getAllUserRentsList(username);
        for (MRentInfo rentInfo : allUserRents)
        {
            if (rentalUid.equals(rentInfo.rentalUid.toString()))
            {
                return rentInfo;
            }
        }
        throw new ENotFoundError("Rent not found!");
    }

    MRentPaymentInfo createPayment(int price)
    {
        String url = UriComponentsBuilder.fromHttpUrl(PaymentService + "/" + Integer.toString(price))
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }

        JSONObject obj = new JSONObject(response.getBody());

        UUID paymentUid = UUID.fromString(obj.getString("v1_payment_uid"));
        String status = obj.getString("v2_status");
        return new MRentPaymentInfo(paymentUid, status, price);
    }

    MRentInfo createRent(String username, String carUid, String paymentUid, String dateFrom, String dateTo)
    {
        String url = UriComponentsBuilder.fromHttpUrl(RentService)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-User-Name", username);
        Map<String, String> values = new HashMap<>();
        values.put("carUid", carUid);
        values.put("paymentUid", paymentUid);
        values.put("dateFrom", dateFrom);
        values.put("dateTo", dateTo);
        HttpEntity<?> entity = new HttpEntity<>(values, headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }

        return getRentInfoFromJSON(new JSONObject(response.getBody()));
    }

    void setCarAvailable(String carUid, boolean isSetAvailable)
    {
        String url = UriComponentsBuilder.fromHttpUrl(
                CarsService + "/" + carUid + "/" + Boolean.toString(isSetAvailable))
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        // headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }

    }

    void cancelRent(String username, String rentalUid)
    {
        String url = UriComponentsBuilder.fromHttpUrl(RentService + "/" + rentalUid + "/cancel")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-User-Name", username);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }
    }

    void finishRent(String username, String rentalUid)
    {
        String url = UriComponentsBuilder.fromHttpUrl(RentService + "/" + rentalUid + "/finish")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-User-Name", username);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }
    }

    void cancelPayment(String paymentUid)
    {
        String url = UriComponentsBuilder.fromHttpUrl(PaymentService + "/" + paymentUid)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            throw new EBadRequestError(e.toString(), new ArrayList<>());
        }
        if (response.getStatusCode() == HttpStatus.NOT_FOUND)
        {
            throw new ENotFoundError(response.getBody());
        }
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST)
        {
            throw new EBadRequestError(response.getBody(), new ArrayList<>());
        }
    }

    String getUsername(String access_token)
    {
        String url = UriComponentsBuilder.fromHttpUrl(AuthService + "/info")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, access_token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            return new String();
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            return new String();
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            return new String();
        }

        JSONObject obj = new JSONObject(response.getBody());
        if (!obj.has("username"))
        {
            return new String();
        }
        return obj.getString("username");
    }

    String getRole(String access_token)
    {
        String url = UriComponentsBuilder.fromHttpUrl(AuthService + "/info")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, access_token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            return new String();
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            return new String();
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            return new String();
        }

        JSONObject obj = new JSONObject(response.getBody());
        if (!obj.has("role"))
        {
            return new String();
        }
        return obj.getString("role");
    }

    boolean IsValidToken(String access_token)
    {
        String url = UriComponentsBuilder.fromHttpUrl(AuthService + "/check")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, access_token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            return false;
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            return false;
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            return false;
        }

        JSONObject obj = new JSONObject(response.getBody());
        if (!obj.has("valid"))
        {
            return false;
        }
        return Boolean.valueOf(obj.getString("valid"));
    }

    JSONObject getUserInfo(String username)
    {
        String url = UriComponentsBuilder.fromHttpUrl(AccService + "/uname/" + username)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            return new JSONObject();
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            return new JSONObject();
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            return new JSONObject();
        }
        return new JSONObject(response.getBody());
    }

    boolean checkUserExists(String username)
    {
        String url = UriComponentsBuilder.fromHttpUrl(AccService + "/uname/" + username)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            return false;
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            return false;
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            return false;
        }
        return response.getStatusCode() == HttpStatus.OK;
    }

    void addUser(String username, String email, String password)
    {
        String url = UriComponentsBuilder.fromHttpUrl(AccService + "/register")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        Map<String, String> values = new HashMap<>();
        values.put("username", username);
        values.put("email", email);
        values.put("password", password);
        HttpEntity<?> entity = new HttpEntity<>(values, headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            return;
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            return;
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            return;
        }
    }

    String getUserToken(String username, String email, String role)
    {
        String url = UriComponentsBuilder.fromHttpUrl(AuthService + "/get")
                .queryParam("username", username)
                .queryParam("email", email)
                .queryParam("role", role)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestOperations restOperations = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response  = restOperations.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        }
        catch (HttpClientErrorException e)
        {
            System.out.println(e);
            return new String();
        }
        catch (HttpServerErrorException e)
        {
            System.out.println(e);
            return new String();
        }
        catch (RestClientException e)
        {
            System.out.println(e);
            return new String();
        }

        JSONObject obj = new JSONObject(response.getBody());
        if (!obj.has("token"))
        {
            return new String();
        }
        return obj.getString("token");
    }

    String fixToken(String token)
    {
        if (token.substring(0, "Bearer ".length()).equals("Bearer "))
        {
            token = token.substring("Bearer ".length());
        }
        return token;
    }
}
