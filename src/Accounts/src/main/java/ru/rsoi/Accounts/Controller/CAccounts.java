package ru.rsoi.Accounts.Controller;

import Utils.AvgTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.rsoi.Accounts.Model.MAccount;
import ru.rsoi.Accounts.Repo.RAccounts;

import ru.rsoi.Accounts.Error.*;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/sys/acc")
public class CAccounts {

    private final RAccounts accRepo;
    private AvgTime avgTime;

    public CAccounts(RAccounts accRepo)
    {
        this.accRepo = accRepo;
        this.avgTime = new AvgTime();
    }

    @GetMapping("")
    public Iterable<MAccount> getAccs()
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        Iterable res = accRepo.findAll();
        avg.end();
        avgTime.add(avg.get());
        return res;
    }

    @GetMapping("/uid/{accUid}")
    public MAccount getAcc(@PathVariable String accUid)
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        MAccount res = findAccByUid(UUID.fromString(accUid))
                .orElseThrow(() -> new EBadRequestError("Acc not found!", new ArrayList<>()));
        avg.end();
        avgTime.add(avg.get());
        return res;
    }

    @GetMapping("/uname/{username}")
    public MAccount getAccByUName(@PathVariable String username)
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        MAccount res = findAccByUName(username)
                .orElseThrow(() -> new EBadRequestError("Acc not found!", new ArrayList<>()));
        avg.end();
        avgTime.add(avg.get());
        return res;
    }

    @GetMapping("/login")
    public MAccount login(@RequestParam String username, @RequestParam String password)
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        MAccount res = findAcc(username, password)
                .orElseThrow(() -> new EBadRequestError("Acc not found!", new ArrayList<>()));
        avg.end();
        avgTime.add(avg.get());
        return res;
    }

    @PostMapping("/register")
    public MAccount addAcc(@RequestBody Map<String, String> values)
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        String username = values.get("username");
        boolean doesExist = findAccByUName(username).isPresent();
        if (doesExist)
        {
            throw new EBadRequestError("User already exists!", new ArrayList<>());
        }

        MAccount acc = new MAccount();
        fillValues(acc, values);
        accRepo.save(acc);
        avg.end();
        avgTime.add(avg.get());
        return acc;
    }

    @PutMapping("/{carUid}")
    public MAccount updateAcc(@PathVariable String carUid, @RequestBody Map<String, String> values)
    {
        AvgTime avg = new AvgTime();
        avg.begin();
        UUID accUidVal = UUID.fromString(carUid);
        Optional<MAccount> oacc = findAccByUid(accUidVal);
        if (!oacc.isPresent())
        {
            throw new ENotFoundError("Acc not found");
        }

        MAccount acc = oacc.get();

        fillValues(acc, values);
        accRepo.deleteById(acc.getId());
        MAccount res = accRepo.save(acc);
        avg.end();
        avgTime.add(avg.get());
        return res;
    }

    @GetMapping("/stats")
    public AvgTime.Info getAvgTime()
    {
        AvgTime.Info res = new AvgTime.Info();
        res.avgTime = avgTime.get();
        return res;
    }

    private Optional<MAccount> findAccByUid(UUID accUid)
    {
        List<MAccount> accs = accRepo.findAccByUid(accUid);
        if (accs.size() == 0)
        {
            return Optional.empty();
        }

        return Optional.of(accs.get(0));
    }

    private Optional<MAccount> findAccByUName(String uname)
    {
        List<MAccount> accs = accRepo.findAccByUName(uname);
        if (accs.size() == 0)
        {
            return Optional.empty();
        }

        return Optional.of(accs.get(0));
    }

    private Optional<MAccount> findAcc(String uname, String password)
    {
        List<MAccount> accs = accRepo.findAcc(uname, password);
        if (accs.size() == 0)
        {
            return Optional.empty();
        }

        return Optional.of(accs.get(0));
    }

    private MAccount fillValues(MAccount acc, Map<String, String> values)
    {
        if (values.containsKey("username"))
        {
            acc.username = values.get("username");
        }
        if (values.containsKey("password")) {
            acc.password = values.get("password");
        }
        if (values.containsKey("email")) {
            acc.email = values.get("email");
        }

        return acc;
    }
}
