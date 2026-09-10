package com.smartcampus.service;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Company;
import com.smartcampus.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<Company> getAll() {
        return companyRepository.findAll();
    }

    public Company getById(String id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
    }

    public Company create(Company company) {
        company.setCreatedAt(LocalDateTime.now());
        return companyRepository.save(company);
    }

    public Company update(String id, Company updates) {
        Company existing = getById(id);
        existing.setName(updates.getName());
        existing.setDescription(updates.getDescription());
        existing.setWebsite(updates.getWebsite());
        existing.setHrContactName(updates.getHrContactName());
        existing.setHrContactEmail(updates.getHrContactEmail());
        existing.setLogoUrl(updates.getLogoUrl());
        return companyRepository.save(existing);
    }

    public void delete(String id) {
        companyRepository.deleteById(id);
    }
}
