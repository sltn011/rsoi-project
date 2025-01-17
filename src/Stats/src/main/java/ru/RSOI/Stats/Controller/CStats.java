package ru.RSOI.Stats.Controller;

import org.springframework.http.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;
import ru.RSOI.Stats.Error.EBadRequestError;
import ru.RSOI.Stats.Error.ENotFoundError;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.RSOI.Stats.Model.MAvgTime;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/sys/stats")
public class CStats {

    public static final String GatewayService = "http://localhost:8080/api/v1/stats";
    public static final String CarsService    = "http://localhost:8070/api/v1/sys/cars/stats";
    public static final String RentService    = "http://localhost:8060/api/v1/sys/rental/stats";
    public static final String PaymentService = "http://localhost:8050/api/v1/sys/payment/stats";
    public static final String AccService     = "http://localhost:8010/api/v1/sys/acc/stats";

    @GetMapping("/avgTime")
    public MAvgTime getAvgTime()
    {
        MAvgTime res = new MAvgTime();
        res.GatewayAvgTime = getServiceAvgTime(GatewayService);
        res.CarsAvgTime = getServiceAvgTime(CarsService);
        res.RentalAvgTime = getServiceAvgTime(RentService);
        res.PaymentAvgTime = getServiceAvgTime(PaymentService);
        res.AccAvgTime = getServiceAvgTime(AccService);
        return res;
    }

    @KafkaListener(topics = "AppTopic", groupId = "AppGroup")
    public void listenGroupFoo(String message) {
        System.out.println("KafkaLog[AppGroup]: " + message);
    }

    private float getServiceAvgTime(String path)
    {
        String url = UriComponentsBuilder.fromHttpUrl(path).toUriString();

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

        return obj.getFloat("avgTime");
    }
}
