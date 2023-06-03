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
@RequestMapping("/api/v1/sys/accounts")
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

    @GetMapping("/{accUid}")
    public MAccount getAcc(@PathVariable String accUid)
    {
        return findAccByUid(UUID.fromString(accUid))
                .orElseThrow(() -> new EBadRequestError("Acc not found!", new ArrayList<>()));
    }

    @PostMapping("")
    public ResponseEntity<Object> addAcc(@RequestBody Map<String, String> values)
    {
        MAccount acc = new MAccount();
        fillValues(acc, values);
        int newID = accRepo.save(acc).getId();

        String stringLocation = String.format("/api/v1/sys/accounts/%d", newID);
        URI location = ServletUriComponentsBuilder.fromUriString(stringLocation).build().toUri();
        return ResponseEntity.created(location).build();
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

    private MAccount fillValues(MAccount acc, Map<String, String> values)
    {
        if (values.containsKey("username"))
        {
            acc.username = values.get("username");
        }
        if (values.containsKey("password")) {
            acc.password = values.get("password");
        }

        return acc;
    }
}
