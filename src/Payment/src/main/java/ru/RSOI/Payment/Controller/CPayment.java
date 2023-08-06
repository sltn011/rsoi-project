package ru.RSOI.Payment.Controller;

import Utils.AvgTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import ru.RSOI.Payment.Error.EBadRequestError;
import ru.RSOI.Payment.Error.ENotFoundError;
import ru.RSOI.Payment.Model.MPayment;
import ru.RSOI.Payment.Repo.RPayment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sys/payment")
public class CPayment {

    private final RPayment paymentRepo;
    private AvgTime avgTime;

    public CPayment(RPayment paymentRepo)
    {
        this.paymentRepo = paymentRepo;
        this.avgTime = new AvgTime();
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String msg) {
        kafkaTemplate.send("AppTopic", msg);
    }

    @GetMapping("")
    public List<MPayment> getAll()
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        List res = paymentRepo.findAll();
        avg.end();
        avgTime.add(avg.get());
        return res;
    }

    @GetMapping("/{payment_uid}")
    public MPayment getPayments(@PathVariable String payment_uid)
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        sendMessage("getPayment " + payment_uid);
        UUID uid = UUID.fromString(payment_uid);
        MPayment res = findPayment(uid);
        avg.end();
        avgTime.add(avg.get());
        return  res;
    }

    @PostMapping("/{price}")
    public MPayment createPayment(@PathVariable int price)
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        sendMessage("createPayment " + price);
        MPayment payment = new MPayment();
        payment.v2_status = "PAID";
        payment.v3_price = price;
        paymentRepo.save(payment);
        avg.end();
        avgTime.add(avg.get());
        return payment;
    }

    @DeleteMapping("/{payment_uid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelPayment(@PathVariable String payment_uid)
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        sendMessage("cancelPayment " + payment_uid);
        UUID uid = UUID.fromString(payment_uid);
        MPayment payment = findPayment(uid);
        paymentRepo.deleteById(payment.getId());
        payment.v2_status = "CANCELED";
        paymentRepo.save(payment);
        avg.end();
        avgTime.add(avg.get());
    }

    @GetMapping("/stats")
    public AvgTime.Info getAvgTime()
    {
        AvgTime.Info res = new AvgTime.Info();
        res.avgTime = avgTime.get();
        return res;
    }

    private MPayment findPayment(UUID payment_uid)
    {
        List<MPayment> payment = paymentRepo.findPayment(payment_uid);

        if (payment.size() == 0)
        {
            ENotFoundError error = new ENotFoundError("payment not found!");
            throw error;
        }

        return payment.get(0);
    }
}
