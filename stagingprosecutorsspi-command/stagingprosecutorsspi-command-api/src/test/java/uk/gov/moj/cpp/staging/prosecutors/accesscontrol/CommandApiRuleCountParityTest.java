package uk.gov.moj.cpp.staging.prosecutors.accesscontrol;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * J17/J25 parity characterisation (J17 golden master), SPI-M07 / BC-20.
 *
 * Deny assertions in the access-control tests also pass when the kbase loads no rules at all, so pin exactly which
 * rules the COMMAND_API kbase loads. The DRL folder name does not match the kbase packages attribute; the rules load
 * because of the package declared inside each DRL.
 */
class CommandApiRuleCountParityTest {

    @Test
    void spiM07_commandApiKbaseShouldLoadAllFiveAccessControlRules() {
        final KieBase kieBase = KieServices.Factory.get().getKieClasspathContainer().getKieBase("COMMAND_API");

        final List<String> ruleNames = kieBase.getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .collect(toList());

        assertThat(ruleNames, containsInAnyOrder(
                "Command - Rule for receiving a prosecution case file",
                "Command - Rule for submitting a prosecution case file",
                "Command - Rule for prosecution case filtered",
                "Command - Rule for resending a cpp message",
                "Command - Rule for updating a police system id for oi"));
    }
}
