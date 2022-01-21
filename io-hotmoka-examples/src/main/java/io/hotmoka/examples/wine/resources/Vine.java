package io.hotmoka.examples.wine.resources;

import io.hotmoka.examples.wine.staff.Role;
import io.hotmoka.examples.wine.staff.SupplyChain;
import io.hotmoka.examples.wine.staff.Worker;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.util.StorageLinkedList;
import io.takamaka.code.util.StorageList;

import java.time.LocalDate;

import static io.takamaka.code.lang.Takamaka.require;

public final class Vine extends Resource {
    private StorageList<String> fertilizers = new StorageLinkedList<>();
    private StorageList<String> pesticides = new StorageLinkedList<>();
    private long harvest;

    @FromContract
    public Vine(SupplyChain chain, String name, String description, int amount, Resource origin) {
        super(chain, name, description, amount, origin);
        require(((Worker) caller()).getRole() == Role.PRODUCER,
                "Only a Producer can create a new object Vine.");
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void addFertilizer(String fertilizer) {
        fertilizers.add(fertilizer);
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void addPesticide(String pesticide) {
        fertilizers.add(pesticide);
    }

    @FromContract(ExternallyOwnedAccount.class)
    public void setHarvestDate(Integer day, Integer month, Integer year) {
        harvest = LocalDate.of(year, month, day).toEpochDay();
    }
}
