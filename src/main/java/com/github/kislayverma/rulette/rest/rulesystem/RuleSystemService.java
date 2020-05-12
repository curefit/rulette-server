package com.github.kislayverma.rulette.rest.rulesystem;

import com.github.kislayverma.rulette.RuleSystem;
import com.github.kislayverma.rulette.core.data.IDataProvider;
import com.github.kislayverma.rulette.core.exception.RuleConflictException;
import com.github.kislayverma.rulette.core.metadata.RuleSystemMetaData;
import com.github.kislayverma.rulette.rest.exception.BadClientException;
import com.github.kislayverma.rulette.rest.exception.BadServerException;
import com.github.kislayverma.rulette.rest.exception.RuleSystemNotFoundException;
import com.github.kislayverma.rulette.rest.model.PaginatedResult;
import com.github.kislayverma.rulette.rest.provider.DataProviderService;
import com.github.kislayverma.rulette.rest.utils.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RuleSystemService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleSystemService.class);
    @Autowired
    private RuleSystemFactory ruleSystemFactory;
    @Autowired
    private DataProviderService providerService;

    public RuleSystem getRuleSystem(final String ruleSystemName) {
        return ruleSystemFactory.getRuleSystem(ruleSystemName);
    }

    public PaginatedResult<RuleSystemMetaData> getAllRuleSystemMetaData(int pageNum, int pageSize) {
        final List<RuleSystemMetaData> metaDataList = new ArrayList<>();
        ruleSystemFactory.getAllRuleSystems().forEach(rs -> {
            LOGGER.info("Returning metadata for rule system {}", rs.getMetaData().getRuleSystemName());
            metaDataList.add(rs.getMetaData());
        });

        PaginatedResult<RuleSystemMetaData> result = PaginationUtil.getPaginatedData(metaDataList, pageNum, pageSize);

        LOGGER.info("Returning metadata for {} rule systems", result.getTotalRecordCount());
        return result;
    }

    public RuleSystemMetaData getRuleSystemMetadata(String ruleSystemName) {
        return getRuleSystem(ruleSystemName).getMetaData();
    }

    public void reload(String ruleSystemName) {
        try {
            getRuleSystem(ruleSystemName).reload();
        } catch (RuleConflictException ex) {
            throw new BadServerException("Error reloading rule system", ex);
        }
    }

    public void createRuleSystem(RuleSystemMetaData ruleSystemMetaData, String providerName) {
        if (ruleSystemMetaData == null ||
            ruleSystemMetaData.getRuleSystemName() == null ||
            ruleSystemMetaData.getRuleSystemName().trim().isEmpty() ||
            ruleSystemMetaData.getTableName() == null || ruleSystemMetaData.getTableName().trim().isEmpty() ||
            ruleSystemMetaData.getUniqueIdColumnName() == null || ruleSystemMetaData.getUniqueIdColumnName().trim().isEmpty() ||
            ruleSystemMetaData.getUniqueOutputColumnName() == null || ruleSystemMetaData.getUniqueOutputColumnName().trim().isEmpty() ||
            providerName == null || providerName.trim().isEmpty()) {
            throw new BadClientException("Incomplete rule system and provider data given to create new rule system");
        }

        String ruleSystemName = ruleSystemMetaData.getRuleSystemName();
        try {
            if (getRuleSystemMetadata(ruleSystemName) != null) {
                throw new BadClientException("Rule system with given name already exists");
            }
        } catch (RuleSystemNotFoundException ex) {
            // Rule system does not exist - so all is well in this use case
        }

        providerService.insertRuleSystemMetadata(ruleSystemMetaData, providerName);
        ruleSystemFactory.reloadRuleSystem(ruleSystemName);
    }

    public void deleteRuleSystem(String ruleSystemName) {
        if (ruleSystemName == null || ruleSystemName.trim().isEmpty()) {
            throw new BadClientException("No rule system name give to delete");
        }
        providerService.deleteRuleSystemMetadata(ruleSystemName);
        ruleSystemFactory.deleteRuleSystem(ruleSystemName);
    }
}
