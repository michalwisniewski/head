/*
 * Copyright (c) 2005-2010 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */

package org.mifos.customers.business.service;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mifos.accounts.business.AccountBO;
import org.mifos.accounts.business.AccountStateMachines;
import org.mifos.accounts.loan.business.LoanBO;
import org.mifos.accounts.productdefinition.business.LoanOfferingBO;
import org.mifos.accounts.productdefinition.business.SavingsOfferingBO;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.util.helpers.SavingsTestHelper;
import org.mifos.accounts.util.helpers.AccountState;
import org.mifos.accounts.util.helpers.AccountStates;
import org.mifos.accounts.util.helpers.AccountTypes;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.config.AccountingRulesConstants;
import org.mifos.config.ConfigurationManager;
import org.mifos.customers.api.CustomerLevel;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.business.CustomerNoteEntity;
import org.mifos.customers.business.CustomerStatusEntity;
import org.mifos.customers.center.business.CenterBO;
import org.mifos.customers.group.business.GroupBO;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.customers.office.business.OfficecFixture;
import org.mifos.customers.persistence.CustomerPersistence;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.util.helpers.CustomerStatus;
import org.mifos.framework.MifosIntegrationTestCase;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.SystemException;
import org.mifos.framework.hibernate.helper.QueryResult;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.util.helpers.TestObjectFactory;

public class CustomerBusinessServiceIntegrationTest extends MifosIntegrationTestCase {

    private static final Integer THREE = Integer.valueOf(3);
    private static final Integer ONE = Integer.valueOf(1);
    private static final OfficeBO OFFICE = OfficecFixture.createOffice(Short.valueOf("1"));

    private CustomerBO center;
    private GroupBO group;
    private CustomerBO client;
    private AccountBO account;
    private LoanBO groupAccount;
    private LoanBO clientAccount;
    private SavingsBO clientSavingsAccount;
    private MeetingBO meeting;
    private final SavingsTestHelper helper = new SavingsTestHelper();
    private SavingsOfferingBO savingsOffering;
    private SavingsBO savingsBO;
    private CustomerBusinessService service;
    private CustomerPersistence customerPersistenceMock;
    private CustomerBusinessService customerBusinessServiceWithMock;

    @Before
    public void setUp() throws Exception {
        service = new CustomerBusinessService();
        customerPersistenceMock = createMock(CustomerPersistence.class);
        customerBusinessServiceWithMock = new CustomerBusinessService(customerPersistenceMock);
    }

    @After
    public void tearDown() throws Exception {
        try {
            // if there is an additional currency code defined, then clear it
            ConfigurationManager.getInstance().clearProperty(AccountingRulesConstants.ADDITIONAL_CURRENCY_CODES);
            clientSavingsAccount = null;
            groupAccount = null;
            clientAccount = null;
            account = null;
            savingsBO = null;
            client = null;
            group = null;
            center = null;
            StaticHibernateUtil.flushSession();
        } catch (Exception e) {
            // throwing here tends to mask other failures
            e.printStackTrace();
        }
    }

    @Test
    public void testSearchGropAndClient() throws Exception {
        createInitialCustomers();
        QueryResult queryResult = new CustomerBusinessService().searchGroupClient("cl", Short.valueOf("1"));
        Assert.assertNotNull(queryResult);
        Assert.assertEquals(1, queryResult.getSize());
        Assert.assertEquals(1, queryResult.get(0, 10).size());

    }

    @Test
    public void testSearchCustForSavings() throws Exception {
        createInitialCustomers();
        QueryResult queryResult = new CustomerBusinessService().searchCustForSavings("c", Short.valueOf("1"));
        Assert.assertNotNull(queryResult);
        Assert.assertEquals(2, queryResult.getSize());
        Assert.assertEquals(2, queryResult.get(0, 10).size());

    }

    @Test
    public void testFindBySystemId() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group_Active_test", CustomerStatus.GROUP_ACTIVE,
                center);
        savingsBO = getSavingsAccount(group, "fsaf5", "ads5");
        StaticHibernateUtil.flushAndClearSession();
        group = (GroupBO) service.findBySystemId(group.getGlobalCustNum());
        Assert.assertEquals("Group_Active_test", group.getDisplayName());
        Assert.assertEquals(2, group.getAccounts().size());
        Assert.assertEquals(0, group.getOpenLoanAccounts().size());
        Assert.assertEquals(1, group.getOpenSavingAccounts().size());
        Assert.assertEquals(CustomerStatus.GROUP_ACTIVE, group.getStatus());
        StaticHibernateUtil.flushSession();
        savingsBO = TestObjectFactory.getObject(SavingsBO.class, savingsBO.getAccountId());
        center = TestObjectFactory.getCenter(center.getCustomerId());
        group = TestObjectFactory.getGroup(group.getCustomerId());
    }

    @Test
    public void testSuccessfulGet() throws Exception {
        center = createCenter("MyCenter");
        savingsBO = getSavingsAccount(center, "fsaf5", "ads5");
        StaticHibernateUtil.flushAndClearSession();
        center = service.getCustomer(center.getCustomerId());
        Assert.assertNotNull(center);
        Assert.assertEquals("MyCenter", center.getDisplayName());
        Assert.assertEquals(2, center.getAccounts().size());
        Assert.assertEquals(0, center.getOpenLoanAccounts().size());
        Assert.assertEquals(1, center.getOpenSavingAccounts().size());
        Assert.assertEquals(CustomerStatus.CENTER_ACTIVE.getValue(), center.getCustomerStatus().getId());
        StaticHibernateUtil.flushSession();
        savingsBO = TestObjectFactory.getObject(SavingsBO.class, savingsBO.getAccountId());
        center = TestObjectFactory.getCenter(center.getCustomerId());
    }

    @Test
    public void testRetrieveAllCustomerStatusList() throws NumberFormatException, SystemException, ApplicationException {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        Assert.assertEquals(2, service.retrieveAllCustomerStatusList(center.getCustomerLevel().getId()).size());
    }

    @Test
    public void testGetAllCustomerNotes() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        center.addCustomerNotes(TestObjectFactory.getCustomerNote("Test Note", center));
        TestObjectFactory.updateObject(center);
        Assert.assertEquals(1, service.getAllCustomerNotes(center.getCustomerId()).getSize());
        for (CustomerNoteEntity note : center.getCustomerNotes()) {
            Assert.assertEquals("Test Note", note.getComment());
            Assert.assertEquals(center.getPersonnel().getPersonnelId(), note.getPersonnel().getPersonnelId());
        }
        center = (CenterBO) (StaticHibernateUtil.getSessionTL().get(CenterBO.class,
                Integer.valueOf(center.getCustomerId())));
    }

    @Test
    public void testGetAllCustomerNotesWithZeroNotes() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("Center_Active_test", meeting);
        Assert.assertEquals(0, service.getAllCustomerNotes(center.getCustomerId()).getSize());
        Assert.assertEquals(0, center.getCustomerNotes().size());
    }

    @Test
    public void testGetStatusList() throws Exception {
        createInitialCustomers();
        AccountStateMachines.getInstance().initialize(AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.CENTER);
        List<CustomerStatusEntity> statusListForCenter = service.getStatusList(center.getCustomerStatus(),
                CustomerLevel.CENTER, TestObjectFactory.TEST_LOCALE);
        Assert.assertEquals(1, statusListForCenter.size());

        AccountStateMachines.getInstance().initialize(AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.GROUP);
        List<CustomerStatusEntity> statusListForGroup = service.getStatusList(group.getCustomerStatus(),
                CustomerLevel.GROUP, TestObjectFactory.TEST_LOCALE);
        Assert.assertEquals(2, statusListForGroup.size());

        AccountStateMachines.getInstance().initialize(AccountTypes.CUSTOMER_ACCOUNT, CustomerLevel.CLIENT);
        List<CustomerStatusEntity> statusListForClient = service.getStatusList(client.getCustomerStatus(),
                CustomerLevel.CLIENT, TestObjectFactory.TEST_LOCALE);
        Assert.assertEquals(2, statusListForClient.size());
    }

    @Test
    public void testSearch() throws Exception {

        center = createCenter("MyCenter");
        QueryResult queryResult = service
                .search("MyCenter", Short.valueOf("3"), Short.valueOf("1"), Short.valueOf("1"));
        Assert.assertNotNull(queryResult);
        Assert.assertEquals(1, queryResult.getSize());
    }

    @Test
    public void testGetActiveCentersUnderUser() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("center", meeting, Short.valueOf("1"), Short.valueOf("1"));
        PersonnelBO personnel = TestObjectFactory.getPersonnel(Short.valueOf("1"));
        List<CustomerBO> customers = service.getActiveCentersUnderUser(personnel);
        Assert.assertNotNull(customers);
        Assert.assertEquals(1, customers.size());
    }

    @Test
    public void testgetGroupsUnderUser() throws Exception {
        MeetingBO meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        center = TestObjectFactory.createWeeklyFeeCenter("center", meeting, Short.valueOf("1"), Short.valueOf("1"));
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group", CustomerStatus.GROUP_ACTIVE, center);
        PersonnelBO personnel = TestObjectFactory.getPersonnel(Short.valueOf("1"));
        List<CustomerBO> customers = service.getGroupsUnderUser(personnel);
        Assert.assertNotNull(customers);
        Assert.assertEquals(1, customers.size());
    }

    @Test
    public void testGetCustomersByLevelId() throws Exception {
        createInitialCustomers();

        List<CustomerBO> client = service.getCustomersByLevelId(Short.parseShort("1"));
        Assert.assertNotNull(client);
        Assert.assertEquals(1, client.size());

        List<CustomerBO> group = service.getCustomersByLevelId(Short.parseShort("2"));
        Assert.assertNotNull(group);
        Assert.assertEquals(1, group.size());

        List<CustomerBO> center = service.getCustomersByLevelId(Short.parseShort("3"));
        Assert.assertNotNull(center);
        Assert.assertEquals(1, client.size());

    }

    private void createInitialCustomers() throws Exception {
        center = createCenter("Center_Active_test");
        group = TestObjectFactory.createWeeklyFeeGroupUnderCenter("Group", CustomerStatus.GROUP_ACTIVE, center);
        client = TestObjectFactory.createClient("client", CustomerStatus.CLIENT_ACTIVE, group);
    }

    private CenterBO createCenter(String name) throws Exception {
        meeting = TestObjectFactory.createMeeting(TestObjectFactory.getTypicalMeeting());
        return TestObjectFactory.createWeeklyFeeCenter(name, meeting);
    }

    private SavingsBO getSavingsAccount(CustomerBO customerBO, String offeringName, String shortName) throws Exception {
        savingsOffering = helper.createSavingsOffering(offeringName, shortName);
        return TestObjectFactory.createSavingsAccount("000100000000017", customerBO,
                AccountStates.SAVINGS_ACC_APPROVED, new Date(System.currentTimeMillis()), savingsOffering);
    }

    private void getCustomer() throws Exception {
        Date startDate = new Date(System.currentTimeMillis());

        createInitialCustomers();
        LoanOfferingBO loanOffering1 = TestObjectFactory.createLoanOffering("Loanwer", "43fs", startDate, meeting);
        LoanOfferingBO loanOffering2 = TestObjectFactory.createLoanOffering("Loancd123", "vfr", startDate, meeting);
        groupAccount = TestObjectFactory.createLoanAccount("42423142341", group,
                AccountState.LOAN_ACTIVE_IN_GOOD_STANDING, startDate, loanOffering1);
        clientAccount = TestObjectFactory.createLoanAccount("3243", client, AccountState.LOAN_ACTIVE_IN_GOOD_STANDING,
                startDate, loanOffering2);
        clientSavingsAccount = getSavingsAccount(client, "SavingPrd11", "abc2");
    }

    @Test
    public void testDropOutRate() throws Exception {
        expect(customerPersistenceMock.getDropOutClientsCountForOffice(OFFICE)).andReturn(ONE);
        expect(customerPersistenceMock.getActiveOrHoldClientCountForOffice(OFFICE)).andReturn(THREE);
        replay(customerPersistenceMock);
        BigDecimal dropOutRate = customerBusinessServiceWithMock.getClientDropOutRateForOffice(OFFICE);
        verify(customerPersistenceMock);
        Assert.assertEquals(25d, dropOutRate.doubleValue(), 0.001);
    }

    @Test
    public void testVeryPoorClientDropoutRate() throws Exception {
        expect(customerPersistenceMock.getVeryPoorDropOutClientsCountForOffice(OFFICE)).andReturn(ONE);
        expect(customerPersistenceMock.getVeryPoorActiveOrHoldClientCountForOffice(OFFICE)).andReturn(THREE);
        replay(customerPersistenceMock);
        BigDecimal veryPoorClientDropoutRateForOffice = customerBusinessServiceWithMock
                .getVeryPoorClientDropoutRateForOffice(OFFICE);
        Assert.assertEquals(25d, veryPoorClientDropoutRateForOffice.doubleValue(), 0.001);
        verify(customerPersistenceMock);
    }
}
