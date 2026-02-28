package com.cargoapp.backend.branches.config;

import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class BranchSeeder implements CommandLineRunner {

    private final BranchRepository branchRepository;

    @Override
    public void run(String... args) {
        if (branchRepository.count() > 0) {
            return;
        }

        BranchEntity bishkek = new BranchEntity();
        bishkek.setAddress("Бишкек, ул. Манаса 40");
        bishkek.setPersonalCodePrefix("AN");

        BranchEntity osh = new BranchEntity();
        osh.setAddress("Ош, ул. Ленина 15");
        osh.setPersonalCodePrefix("D");

        branchRepository.save(bishkek);
        branchRepository.save(osh);
    }
}
