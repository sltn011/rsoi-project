package ru.rsoi.Accounts.Controller;

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

    public CAccounts(RAccounts accRepo)
    {
        this.accRepo = accRepo;
    }

    @GetMapping("")
    public Iterable<MAccount> getAccs()
    {
        return accRepo.findAll();
    }

    @GetMapping("/uid/{accUid}")
    public MAccount getAcc(@PathVariable String accUid)
    {
        return findAccByUid(UUID.fromString(accUid))
                .orElseThrow(() -> new EBadRequestError("Acc not found!", new ArrayList<>()));
    }

    @GetMapping("/uname/{username}")
    public MAccount getAccByUName(@PathVariable String username)
    {
        return findAccByUName(username)
                .orElseThrow(() -> new EBadRequestError("Acc not found!", new ArrayList<>()));
    }

    @GetMapping("/login")
    public MAccount login(@RequestParam String username, @RequestParam String password)
    {
        return findAcc(username, password)
                .orElseThrow(() -> new EBadRequestError("Acc not found!", new ArrayList<>()));
    }

    @PostMapping("/register")
    public MAccount addAcc(@RequestBody Map<String, String> values)
    {
        String username = values.get("username");
        boolean doesExist = findAccByUName(username).isPresent();
        if (doesExist)
        {
            throw new EBadRequestError("User already exists!", new ArrayList<>());
        }

        MAccount acc = new MAccount();
        fillValues(acc, values);
        accRepo.save(acc);
        return acc;
    }

    @PutMapping("/{carUid}")
    public MAccount updateAcc(@PathVariable String carUid, @RequestBody Map<String, String> values)
    {
        UUID accUidVal = UUID.fromString(carUid);
        Optional<MAccount> oacc = findAccByUid(accUidVal);
        if (!oacc.isPresent())
        {
            throw new ENotFoundError("Acc not found");
        }

        MAccount acc = oacc.get();

        fillValues(acc, values);
        accRepo.deleteById(acc.getId());
        return accRepo.save(acc);
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
