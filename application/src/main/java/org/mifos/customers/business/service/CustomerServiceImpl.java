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

import java.io.InputStream;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.mifos.accounts.business.AccountFeesEntity;
import org.mifos.accounts.exceptions.AccountException;
import org.mifos.accounts.productdefinition.business.SavingsOfferingBO;
import org.mifos.accounts.productdefinition.util.helpers.RecommendedAmountUnit;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.persistence.SavingsPersistence;
import org.mifos.accounts.util.helpers.AccountState;
import org.mifos.application.holiday.business.Holiday;
import org.mifos.application.holiday.persistence.HolidayDao;
import org.mifos.application.master.MessageLookup;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.master.business.CustomFieldDto;
import org.mifos.application.master.persistence.MasterPersistence;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.servicefacade.CenterUpdate;
import org.mifos.application.servicefacade.ClientFamilyInfoUpdate;
import org.mifos.application.servicefacade.ClientMfiInfoUpdate;
import org.mifos.application.servicefacade.ClientPersonalInfoUpdate;
import org.mifos.application.servicefacade.CustomerStatusUpdate;
import org.mifos.application.servicefacade.DependencyInjectedServiceLocator;
import org.mifos.application.servicefacade.GroupUpdate;
import org.mifos.application.util.helpers.EntityType;
import org.mifos.calendar.CalendarEvent;
import org.mifos.config.FiscalCalendarRules;
import org.mifos.config.util.helpers.ConfigurationConstants;
import org.mifos.core.MifosRuntimeException;
import org.mifos.customers.business.CustomerAccountBO;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.business.CustomerDto;
import org.mifos.customers.business.CustomerHierarchyEntity;
import org.mifos.customers.business.CustomerMeetingEntity;
import org.mifos.customers.business.CustomerNoteEntity;
import org.mifos.customers.business.CustomerPositionDto;
import org.mifos.customers.business.CustomerPositionEntity;
import org.mifos.customers.business.CustomerStatusEntity;
import org.mifos.customers.business.CustomerStatusFlagEntity;
import org.mifos.customers.business.PositionEntity;
import org.mifos.customers.center.business.CenterBO;
import org.mifos.customers.client.business.ClientBO;
import org.mifos.customers.client.business.ClientInitialSavingsOfferingEntity;
import org.mifos.customers.client.persistence.ClientPersistence;
import org.mifos.customers.client.util.helpers.ClientConstants;
import org.mifos.customers.exceptions.CustomerException;
import org.mifos.customers.group.business.GroupBO;
import org.mifos.customers.group.util.helpers.GroupConstants;
import org.mifos.customers.office.business.OfficeBO;
import org.mifos.customers.office.persistence.OfficeDao;
import org.mifos.customers.persistence.CustomerDao;
import org.mifos.customers.persistence.CustomerPersistence;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.persistence.PersonnelDao;
import org.mifos.customers.util.helpers.CustomerConstants;
import org.mifos.customers.util.helpers.CustomerLevel;
import org.mifos.customers.util.helpers.CustomerStatus;
import org.mifos.customers.util.helpers.CustomerStatusFlag;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.InvalidDateException;
import org.mifos.framework.exceptions.PersistenceException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.hibernate.helper.HibernateTransactionHelper;
import org.mifos.framework.hibernate.helper.StaticHibernateUtil;
import org.mifos.framework.util.DateTimeService;
import org.mifos.security.util.UserContext;

/**
 * Default implementation of {@link CustomerService}.
 */
public class CustomerServiceImpl implements CustomerService {

    private final CustomerDao customerDao;
    private final PersonnelDao personnelDao;
    private final OfficeDao officeDao;
    private final HolidayDao holidayDao;
    private final HibernateTransactionHelper hibernateTransactionHelper;
    private CustomerAccountFactory customerAccountFactory = DefaultCustomerAccountFactory.createNew();

    public CustomerServiceImpl(CustomerDao customerDao, PersonnelDao personnelDao, OfficeDao officeDao,
            HolidayDao holidayDao, final HibernateTransactionHelper hibernateTransactionHelper) {
        this.customerDao = customerDao;
        this.personnelDao = personnelDao;
        this.officeDao = officeDao;
        this.holidayDao = holidayDao;
        this.hibernateTransactionHelper = hibernateTransactionHelper;
    }

    public void setCustomerAccountFactory(CustomerAccountFactory customerAccountFactory) {
        this.customerAccountFactory = customerAccountFactory;
    }

    @Override
    public final void createCenter(CenterBO customer, MeetingBO meeting, List<AccountFeesEntity> accountFees) throws ApplicationException {

        customer.validate();
        customer.validateMeetingAndFees(accountFees);

//        FIXME - keithw - should we ensure center names are unique per branch/office
//        customerDao.validateGroupNameIsNotTakenForOffice(group.getDisplayName(), group.getOffice().getOfficeId());

        List<CustomFieldDefinitionEntity> allCustomFieldsForCenter = customerDao.retrieveCustomFieldEntitiesForCenter();
        customer.validateMandatoryCustomFields(allCustomFieldsForCenter);

        // FIXME - #000003 - keithw - mandatory configurable fields are not validated in customer creation (center, group, client)
        // List<FieldConfigurationEntity> mandatoryConfigurableFields = this.customerDao.findMandatoryConfigurableFieldsApplicableToCenter();

        createCustomer(customer, meeting, accountFees);
    }

    @Override
    public final void createGroup(GroupBO group, MeetingBO meeting, List<AccountFeesEntity> accountFees)
            throws CustomerException {

        group.validate();

        customerDao.validateGroupNameIsNotTakenForOffice(group.getDisplayName(), group.getOffice().getOfficeId());

        List<CustomFieldDefinitionEntity> allCustomFieldsForGroup = customerDao.retrieveCustomFieldEntitiesForGroup();
        group.validateMandatoryCustomFields(allCustomFieldsForGroup);

        // FIXME - #000003 - keithw - mandatory configurable fields are not validated in customer creation (center, group, client)
        // List<FieldConfigurationEntity> mandatoryConfigurableFields = this.customerDao.findMandatoryConfigurableFieldsApplicableToCenter();

        createCustomer(group, meeting, accountFees);
    }

    @Override
    public final void createClient(ClientBO client, MeetingBO meeting, List<AccountFeesEntity> accountFees,
            List<SavingsOfferingBO> savingProducts) throws CustomerException {

        if (client.isStatusValidationRequired()) {
            client.validateClientStatus();
        }

        client.validateOffice();
        // FIXME - #00003 - keithw verify validation here when creating clients
        // client.validateOfferings();
        // client.validateForDuplicateNameOrGovtId(displayName, dateOfBirth, governmentId);

        if (client.isActive()) {
            client.validateFieldsForActiveClient();

            UserContext userContext = client.getUserContext();

            List<SavingsBO> savingsAccounts = new ArrayList<SavingsBO>();
            for (SavingsOfferingBO clientSavingsProduct : savingProducts) {
                try {
                    if (clientSavingsProduct.isActive()) {
                        List<CustomFieldDefinitionEntity> customFieldDefs = new SavingsPersistence()
                                .retrieveCustomFieldsDefinition(EntityType.SAVINGS.getValue());
                        List<CustomFieldDto> savingCustomFieldViews = CustomFieldDefinitionEntity.toDto(
                                customFieldDefs, userContext.getPreferredLocale());

                        SavingsBO savingsAccount = new SavingsBO(userContext, clientSavingsProduct, client,
                                AccountState.SAVINGS_ACTIVE, clientSavingsProduct.getRecommendedAmount(),
                                savingCustomFieldViews);
                        savingsAccounts.add(savingsAccount);
                    }
                } catch (PersistenceException pe) {
                    throw new MifosRuntimeException(pe);
                } catch (AccountException pe) {
                    throw new MifosRuntimeException(pe);
                }
            }

            client.addSavingsAccounts(savingsAccounts);

            List<Days> workingDays = new FiscalCalendarRules().getWorkingDaysAsJodaTimeDays();
            List<Holiday> holidays = new ArrayList<Holiday>();

            try {
                CustomerBO parentCustomer = client.getParentCustomer();
                if (parentCustomer != null) {
                    List<SavingsBO> groupSavingAccounts = new CustomerPersistence()
                            .retrieveSavingsAccountForCustomer(parentCustomer.getCustomerId());

                    CustomerBO grandParentCustomer = parentCustomer.getParentCustomer();
                    if (grandParentCustomer != null) {
                        List<SavingsBO> centerSavingAccounts = new CustomerPersistence()
                                .retrieveSavingsAccountForCustomer(grandParentCustomer.getCustomerId());
                        groupSavingAccounts.addAll(centerSavingAccounts);
                    }

                    for (SavingsBO savings : groupSavingAccounts) {
                        savings.setUserContext(userContext);

                        if (client.getCustomerMeetingValue() != null) {

                            if (!(savings.getCustomer().getLevel() == CustomerLevel.GROUP && savings
                                    .getRecommendedAmntUnit().getId().equals(
                                            RecommendedAmountUnit.COMPLETE_GROUP.getValue()))) {
                                savings.generateDepositAccountActions(client, client.getCustomerMeeting().getMeeting(),
                                        workingDays, holidays);
                            }
                        }
                    }
                }
            } catch (PersistenceException pe) {
                throw new MifosRuntimeException(pe);
            }
        }

        client.generateSearchId();

        createCustomer(client, meeting, accountFees);
    }

    private void createCustomer(CustomerBO customer, MeetingBO meeting, List<AccountFeesEntity> accountFees) {
        try {
            // in case any other leaked sessions exist from legacy code use.
            this.hibernateTransactionHelper.closeSession();

            this.hibernateTransactionHelper.startTransaction();
            this.customerDao.save(customer);

            CalendarEvent upcomingCalendarEvents = this.holidayDao.findCalendarEventsForThisYearAndNext();
            CustomerAccountBO customerAccount = this.customerAccountFactory.create(customer, accountFees, meeting, upcomingCalendarEvents);
            customer.addAccount(customerAccount);

            this.customerDao.save(customer);
            this.hibernateTransactionHelper.commitTransaction();

            this.hibernateTransactionHelper.startTransaction();
            customer.generateGlobalCustomerNumber();
            this.customerDao.save(customer);

            if (customer.getParentCustomer() != null) {
                this.customerDao.save(customer.getParentCustomer());
            }

            this.hibernateTransactionHelper.commitTransaction();
        } catch (Exception e) {
            this.hibernateTransactionHelper.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            this.hibernateTransactionHelper.closeSession();
        }
    }

    @Override
    public final void updateCenter(UserContext userContext, CenterUpdate centerUpdate) throws ApplicationException {

        CustomerBO center = customerDao.findCustomerById(centerUpdate.getCustomerId());
        center.validateVersion(centerUpdate.getVersionNum());
        center.setUserContext(userContext);

        List<CustomFieldDefinitionEntity> allCustomFieldsForCenter = customerDao.retrieveCustomFieldEntitiesForCenter();
        center.updateCustomFields(centerUpdate.getCustomFields());
        center.validateMandatoryCustomFields(allCustomFieldsForCenter);

        assembleCustomerPostionsFromDto(centerUpdate.getCustomerPositions(), center);

        // FIXME - #000003 - keithw - mandatory configurable fields are not validated in customer update (center, group,
        // client)
        // List<FieldConfigurationEntity> mandatoryConfigurableFields =
        // this.customerDao.findMandatoryConfigurableFieldsApplicableToCenter();

        try {
            hibernateTransactionHelper.startTransaction();
            hibernateTransactionHelper.beginAuditLoggingFor(center);

            updateLoanOfficerAndValidate(centerUpdate.getLoanOfficerId(), center);

            center.updateCenterDetails(userContext, centerUpdate);

            customerDao.save(center);

            hibernateTransactionHelper.commitTransaction();
        } catch (ApplicationException e) {
            hibernateTransactionHelper.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            hibernateTransactionHelper.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            hibernateTransactionHelper.closeSession();
        }
    }

    @Override
    public final void updateGroup(UserContext userContext, GroupUpdate groupUpdate) throws ApplicationException {

        GroupBO group = customerDao.findGroupBySystemId(groupUpdate.getGlobalCustNum());
        group.validateVersion(groupUpdate.getVersionNo());
        group.setUserContext(userContext);

        List<CustomFieldDefinitionEntity> allCustomFieldsForGroup = customerDao.retrieveCustomFieldEntitiesForGroup();
        group.updateCustomFields(groupUpdate.getCustomFields());
        group.validateMandatoryCustomFields(allCustomFieldsForGroup);

        assembleCustomerPostionsFromDto(groupUpdate.getCustomerPositions(), group);

        try {
            hibernateTransactionHelper.startTransaction();
            hibernateTransactionHelper.beginAuditLoggingFor(group);

            group.updateTrainedDetails(groupUpdate);
            group.setExternalId(groupUpdate.getExternalId());
            group.updateAddress(groupUpdate.getAddress());

            if (group.isNameDifferent(groupUpdate.getDisplayName())) {
                customerDao.validateGroupNameIsNotTakenForOffice(groupUpdate.getDisplayName(), group.getOffice().getOfficeId());
                group.setDisplayName(groupUpdate.getDisplayName());
            }

            updateLoanOfficerAndValidate(groupUpdate.getLoanOfficerId(), group);

            customerDao.save(group);

            hibernateTransactionHelper.commitTransaction();
        } catch (ApplicationException e) {
            hibernateTransactionHelper.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            hibernateTransactionHelper.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            hibernateTransactionHelper.closeSession();
        }
    }

    @Override
    public final void updateClientPersonalInfo(ClientBO client, ClientPersonalInfoUpdate personalInfo) throws InvalidDateException {

        setInitialObjectForAuditLogging(client);
        client.updatePersonalInfo(personalInfo);

        try {
            StaticHibernateUtil.startTransaction();
            InputStream pictureSteam = personalInfo.getPicture();

            if (pictureSteam != null) {
                Blob pictureAsBlob = new ClientPersistence().createBlob(pictureSteam);
                client.createOrUpdatePicture(pictureAsBlob);
            }

            customerDao.save(client);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            StaticHibernateUtil.closeSession();
        }
    }

    @Override
    public final void updateClientFamilyInfo(ClientBO client, ClientFamilyInfoUpdate clientFamilyInfoUpdate) {

        try {
            setInitialObjectForAuditLogging(client);
            client.updateFamilyInfo(clientFamilyInfoUpdate);
        } catch (PersistenceException e) {
            throw new MifosRuntimeException(e);
        }

        try {
            StaticHibernateUtil.startTransaction();
            customerDao.save(client);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            StaticHibernateUtil.closeSession();
        }
    }

    @Override
    public final void updateClientMfiInfo(ClientBO client, ClientMfiInfoUpdate clientMfiInfoUpdate) throws CustomerException {

        client.setExternalId(clientMfiInfoUpdate.getExternalId());
        client.setTrained(clientMfiInfoUpdate.isTrained());
        client.setTrainedDate(clientMfiInfoUpdate.getTrainedDate().toDate());

        setInitialObjectForAuditLogging(client);

        PersonnelBO personnel = this.personnelDao.findPersonnelById(clientMfiInfoUpdate.getPersonnelId());
        client.updateMfiInfo(personnel);

        try {
            StaticHibernateUtil.startTransaction();
            customerDao.save(client);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            StaticHibernateUtil.closeSession();
        }
    }

    private void updateLoanOfficerAndValidate(Short loanOfficerId, CustomerBO customer) throws CustomerException {
        PersonnelBO loanOfficer = personnelDao.findPersonnelById(loanOfficerId);

        if (customer.isLoanOfficerChanged(loanOfficer)) {
            customer.setLoanOfficer(loanOfficer);
            customer.validate();

            customerDao.updateLoanOfficersForAllChildrenAndAccounts(loanOfficerId, customer.getSearchId(), customer.getOffice().getOfficeId());
        } else {
            customer.validate();
        }
    }

    private void assembleCustomerPostionsFromDto(List<CustomerPositionDto> customerPositions, CustomerBO customer) {

        for (CustomerPositionDto positionView : customerPositions) {
            boolean isPositionFound = false;

            for (CustomerPositionEntity positionEntity : customer.getCustomerPositions()) {

                if (positionView.getPositionId().equals(positionEntity.getPosition().getId())) {

                    CustomerBO customerInPosition = customerDao.findCustomerById(positionView.getCustomerId());
                    positionEntity.setCustomer(customerInPosition);
                    isPositionFound = true;
                    break;
                }
            }
            if (!isPositionFound) {

                CustomerBO customerInPosition = customerDao.findCustomerById(positionView.getCustomerId());
                CustomerPositionEntity customerPosition = new CustomerPositionEntity(new PositionEntity(positionView
                        .getPositionId()), customerInPosition, customer);
                customer.addCustomerPosition(customerPosition);
            }
        }
    }

    @Override
    public final void updateCustomerStatus(UserContext userContext, CustomerStatusUpdate customerStatusUpdate) throws CustomerException {

        CustomerBO customer = this.customerDao.findCustomerById(customerStatusUpdate.getCustomerId());
        customer.validateVersion(customerStatusUpdate.getVersionNum());
        customer.setUserContext(userContext);
        customer.setUpdateDetails();

        checkPermission(customer, userContext, customerStatusUpdate.getNewStatus(), customerStatusUpdate.getCustomerStatusFlag());

        Short oldStatusId = customer.getCustomerStatus().getId();
        CustomerStatus oldStatus = CustomerStatus.fromInt(oldStatusId);

//        customer.validateLoanOfficerIsActive();

        PersonnelBO loggedInUser = this.personnelDao.findPersonnelById(userContext.getId());

        CustomerNoteEntity customerNote = new CustomerNoteEntity(customerStatusUpdate.getNotes(), new DateTimeService().getCurrentJavaSqlDate(), loggedInUser, customer);

        if (customer.isGroup()) {
            GroupBO group = (GroupBO) customer;
            updateGroupStatus(group, oldStatus, customerStatusUpdate.getNewStatus(), customerStatusUpdate.getCustomerStatusFlag(), customerNote);
        } else if (customer.isClient()) {
            ClientBO client = (ClientBO) customer;
            updateClientStatus(client, oldStatus, customerStatusUpdate.getNewStatus(), customerStatusUpdate.getCustomerStatusFlag(), customerNote);
        } else {
            CenterBO center = (CenterBO) customer;
            updateCenterStatus(center, customerStatusUpdate.getNewStatus(), customerStatusUpdate.getCustomerStatusFlag(), customerNote);
        }
    }

    private void checkPermission(CustomerBO customerBO, UserContext userContext, CustomerStatus newStatus, CustomerStatusFlag statusFlag) {
        try {

            Short statusFlagId = null;
            if (statusFlag != null) {
                statusFlagId = statusFlag.getValue();
            }

            if (null != customerBO.getPersonnel()) {
                new CustomerBusinessService().checkPermissionForStatusChange(newStatus.getValue(), userContext, statusFlagId,
                        customerBO.getOffice().getOfficeId(), customerBO.getPersonnel().getPersonnelId());
            } else {
                new CustomerBusinessService().checkPermissionForStatusChange(newStatus.getValue(), userContext, statusFlagId,
                        customerBO.getOffice().getOfficeId(), userContext.getId());
            }
        } catch (ServiceException e) {
            throw new MifosRuntimeException(e);
        }
    }

    @Override
    public final void updateCenterStatus(CenterBO center, CustomerStatus newStatus, CustomerStatusFlag customerStatusFlag,
            CustomerNoteEntity customerNote) throws CustomerException {

        if (newStatus.isCenterInActive()) {

            center.validateChangeToInActive();

            List<CustomerDto> clientsThatAreNotClosedOrCanceled = this.customerDao
                    .findClientsThatAreNotCancelledOrClosed(center.getSearchId(), center.getOffice().getOfficeId());

            List<CustomerDto> groupsThatAreNotClosedOrCancelled = this.customerDao
                    .findGroupsThatAreNotCancelledOrClosed(center.getSearchId(), center.getOffice().getOfficeId());

            if (clientsThatAreNotClosedOrCanceled.size() > 0 || groupsThatAreNotClosedOrCancelled.size() > 0) {
                throw new CustomerException(CustomerConstants.ERROR_STATE_CHANGE_EXCEPTION,
                        new Object[] { MessageLookup.getInstance().lookupLabel(ConfigurationConstants.GROUP,
                                center.getUserContext()) });
            }

        } else if (newStatus.isCenterActive()) {
            center.validateChangeToActive();
            center.validateLoanOfficerIsActive();
        }

        CustomerStatusFlagEntity customerStatusFlagEntity = populateCustomerStatusFlag(customerStatusFlag);

        try {
            StaticHibernateUtil.startTransaction();
            setInitialObjectForAuditLogging(center);
            center.clearCustomerFlagsIfApplicable(center.getStatus(), newStatus);
            center.updateCustomerStatus(newStatus);
            center.addCustomerNotes(customerNote);
            if (customerStatusFlagEntity != null) {
                center.addCustomerFlag(customerStatusFlagEntity);
            }

            customerDao.save(center);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            StaticHibernateUtil.closeSession();
        }
    }

    @Override
    public final void updateGroupStatus(GroupBO group, CustomerStatus oldStatus, CustomerStatus newStatus,
            CustomerStatusFlag customerStatusFlag, CustomerNoteEntity customerNote) throws CustomerException {

        validateChangeOfStatusForGroup(group, oldStatus, newStatus);

        CustomerStatusFlagEntity customerStatusFlagEntity = populateCustomerStatusFlag(customerStatusFlag);

        try {
            StaticHibernateUtil.startTransaction();
            setInitialObjectForAuditLogging(group);

            group.clearCustomerFlagsIfApplicable(oldStatus, newStatus);

            if (group.isActiveForFirstTime(oldStatus.getValue(), newStatus.getValue())) {
                group.setCustomerActivationDate(new DateTime().toDate());

                if (group.getParentCustomer() != null) {
                    CustomerHierarchyEntity hierarchy = new CustomerHierarchyEntity(group, group.getParentCustomer());
                    group.addCustomerHierarchy(hierarchy);
                }

                group.getCustomerAccount().generateCustomerFeeSchedule();
            }

            Set<CustomerBO> groupChildren = group.getChildren();

            if (oldStatus.isGroupPending() && newStatus.isGroupCancelled() && groupChildren != null) {

                for (CustomerBO child : groupChildren) {

                    ClientBO client = (ClientBO) child;

                    if (client.isPending()) {
                        client.setUserContext(group.getUserContext());
                        client.updateCustomerStatus(CustomerStatus.CLIENT_PARTIAL);
                        changeClientStatus(client, customerStatusFlag, customerNote);
                        customerDao.save(client);
                    }
                }
            }

            group.updateCustomerStatus(newStatus);
            group.addCustomerNotes(customerNote);
            if (customerStatusFlagEntity != null) {
                group.addCustomerFlag(customerStatusFlagEntity);
            }

            customerDao.save(group);
            StaticHibernateUtil.commitTransaction();

        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            StaticHibernateUtil.closeSession();
        }
    }

    private CustomerStatusFlagEntity populateCustomerStatusFlag(CustomerStatusFlag customerStatusFlag)
            throws CustomerException {
        CustomerStatusFlagEntity customerStatusFlagEntity = null;
        if (customerStatusFlag != null) {
            try {
                customerStatusFlagEntity = (CustomerStatusFlagEntity) new MasterPersistence().getPersistentObject(
                        CustomerStatusFlagEntity.class, customerStatusFlag.getValue());
            } catch (PersistenceException e) {
                throw new CustomerException(e);
            }
        }
        return customerStatusFlagEntity;
    }

    private void validateChangeOfStatusForGroup(GroupBO group, CustomerStatus oldStatus, CustomerStatus newStatus)
            throws CustomerException {

        if (newStatus.isGroupClosed()) {
            group.validateNoActiveAccountExist();

            List<CustomerDto> clientsThatAreNotClosedOrCanceled = this.customerDao
                    .findClientsThatAreNotCancelledOrClosed(group.getSearchId(), group.getOffice().getOfficeId());

            if (clientsThatAreNotClosedOrCanceled.size() > 0) {
                throw new CustomerException(CustomerConstants.ERROR_STATE_CHANGE_EXCEPTION,
                        new Object[] { MessageLookup.getInstance().lookupLabel(ConfigurationConstants.CLIENT,
                                group.getUserContext()) });
            }
        }

        if (newStatus.isGroupActive()) {
            group.validateGroupCanBeActive();
        }

        if (oldStatus.isGroupCancelled() && newStatus.isGroupPartial()) {
            if (group.getParentCustomer() != null && group.getParentCustomer().getCustomerId() != null) {
                group.validateTransitionFromCancelledToPartialIsAllowedBasedOnCenter();
            } else {
                this.officeDao.validateBranchIsActiveWithNoActivePersonnel(group.getOffice().getOfficeId(), group
                        .getUserContext());
            }
        }
    }

    @Override
    public final void updateClientStatus(ClientBO client, CustomerStatus oldStatus, CustomerStatus newStatus,
            CustomerStatusFlag customerStatusFlag, CustomerNoteEntity customerNote) throws CustomerException {

        handeClientChangeOfStatus(client, newStatus);

        CustomerStatusFlagEntity customerStatusFlagEntity = populateCustomerStatusFlag(customerStatusFlag);

        try {
            StaticHibernateUtil.startTransaction();

            setInitialObjectForAuditLogging(client);
            client.clearCustomerFlagsIfApplicable(oldStatus, newStatus);

            client.updateCustomerStatus(newStatus);
            changeStatus(client, oldStatus, newStatus);

            if (customerStatusFlagEntity != null) {
                client.addCustomerFlag(customerStatusFlagEntity);
            }
            client.addCustomerNotes(customerNote);
            this.changeClientStatus(client, customerStatusFlag, customerNote);

            customerDao.save(client);
            StaticHibernateUtil.commitTransaction();
        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            StaticHibernateUtil.closeSession();
        }
    }

    private void changeClientStatus(ClientBO client, CustomerStatusFlag customerStatusFlag,
            CustomerNoteEntity customerNote) throws AccountException {
        if (client.isClosedOrCancelled()) {

            if (client.isClientUnderGroup()) {

                CustomerBO parentCustomer = client.getParentCustomer();

                client.resetPositions(parentCustomer);
                parentCustomer.setUserContext(client.getUserContext());
                CustomerBO center = parentCustomer.getParentCustomer();
                if (center != null) {
                    parentCustomer.resetPositions(center);
                    center.setUserContext(client.getUserContext());
                }
            }

            CustomerAccountBO customerAccount = client.getCustomerAccount();
            if (customerAccount.isOpen()) {
                customerAccount.setUserContext(client.getUserContext());

                customerAccount.changeStatus(AccountState.CUSTOMER_ACCOUNT_INACTIVE, customerStatusFlag.getValue(),
                        customerNote.getComment());
                customerAccount.update();
            }
        }
    }

    private void changeStatus(CustomerBO customer, CustomerStatus oldStatus, CustomerStatus newStatus) throws CustomerException {
        Short oldStatusId = oldStatus.getValue();
        Short newStatusId = newStatus.getValue();

        if (customer.isClient()) {

            ClientBO client = (ClientBO) customer;

            if (client.isActiveForFirstTime(oldStatusId, newStatusId)) {
                try {
                    if (client.getParentCustomer() != null) {
                        CustomerHierarchyEntity hierarchy = new CustomerHierarchyEntity(client, client.getParentCustomer());
                        client.addCustomerHierarchy(hierarchy);
                    }

                    client.getCustomerAccount().generateCustomerFeeSchedule();
                } catch (AccountException ae1) {
                    throw new CustomerException(ae1);
                }
            }
            if (client.isActiveForFirstTime(oldStatusId, newStatusId)) {
                client.setCustomerActivationDate(new DateTimeService().getCurrentJavaDateTime());

                if (client.getOfferingsAssociatedInCreate() != null) {
                    for (ClientInitialSavingsOfferingEntity clientOffering : client.getOfferingsAssociatedInCreate()) {
                        try {
                            SavingsOfferingBO savingsOffering = client.getSavingsPrdPersistence().getSavingsProduct(
                                    clientOffering.getSavingsOffering().getPrdOfferingId());

                            if (savingsOffering.isActive()) {

                                List<CustomFieldDefinitionEntity> customFieldDefs = client.getSavingsPersistence()
                                        .retrieveCustomFieldsDefinition(EntityType.SAVINGS.getValue());

                                List<CustomFieldDto> customerFieldsForSavings = CustomFieldDefinitionEntity.toDto(
                                        customFieldDefs, customer.getUserContext().getPreferredLocale());

                                client.addAccount(new SavingsBO(client.getUserContext(), savingsOffering, client,
                                        AccountState.SAVINGS_ACTIVE, savingsOffering.getRecommendedAmount(),
                                        customerFieldsForSavings));
                            }
                        } catch (PersistenceException pe) {
                            throw new CustomerException(pe);
                        } catch (AccountException pe) {
                            throw new CustomerException(pe);
                        }
                    }
                }

                new SavingsPersistence().persistSavingAccounts(client);

                List<Days> workingDays = new FiscalCalendarRules().getWorkingDaysAsJodaTimeDays();
                List<Holiday> holidays = DependencyInjectedServiceLocator.locateHolidayDao()
                        .findAllHolidaysThisYearAndNext();

                try {
                    if (client.getParentCustomer() != null) {
                        List<SavingsBO> savingsList = new CustomerPersistence()
                                .retrieveSavingsAccountForCustomer(client.getParentCustomer().getCustomerId());

                        if (client.getParentCustomer().getParentCustomer() != null) {
                            savingsList.addAll(new CustomerPersistence().retrieveSavingsAccountForCustomer(client
                                    .getParentCustomer().getParentCustomer().getCustomerId()));
                        }
                        for (SavingsBO savings : savingsList) {
                            savings.setUserContext(client.getUserContext());

                            if (client.getCustomerMeeting().getMeeting() != null) {
                                if (!(savings.getCustomer().getLevel() == CustomerLevel.GROUP && savings
                                        .getRecommendedAmntUnit().getId().equals(
                                                RecommendedAmountUnit.COMPLETE_GROUP.getValue()))) {

                                    savings.generateDepositAccountActions(client, client.getCustomerMeeting()
                                            .getMeeting(), workingDays, holidays);

                                    savings.update();
                                }
                            }
                        }
                    }

                } catch (PersistenceException pe) {
                    throw new CustomerException(pe);
                } catch (AccountException ae) {
                    throw new CustomerException(ae);
                }
            }
        }
    }

    private void handeClientChangeOfStatus(ClientBO client, CustomerStatus newStatus) throws CustomerException {
        if (client.getParentCustomer() != null) {

            CustomerStatus groupStatus = client.getParentCustomer().getStatus();

            if ((newStatus.isClientActive() || newStatus.isClientPending()) && client.isClientUnderGroup()) {

                if (groupStatus.isGroupCancelled()) {
                    throw new CustomerException(ClientConstants.ERRORS_GROUP_CANCELLED, new Object[] { MessageLookup
                            .getInstance().lookupLabel(ConfigurationConstants.GROUP, client.getUserContext()) });
                }

                if (client.isGroupStatusLower(newStatus.getValue(), groupStatus.getValue())) {

                    throw new CustomerException(ClientConstants.INVALID_CLIENT_STATUS_EXCEPTION, new Object[] {
                            MessageLookup.getInstance().lookupLabel(ConfigurationConstants.GROUP,
                                    client.getUserContext()),
                            MessageLookup.getInstance().lookupLabel(ConfigurationConstants.CLIENT,
                                    client.getUserContext()) });
                }
            }
        }

        if (newStatus.isClientClosed()) {
            if (client.isAnyLoanAccountOpen() || client.isAnySavingsAccountOpen()) {
                throw new CustomerException(CustomerConstants.CUSTOMER_HAS_ACTIVE_ACCOUNTS_EXCEPTION);
            }
        }

        if (newStatus.isClientActive()) {
            if (client.getPersonnel() == null || client.getPersonnel().getPersonnelId() == null) {
                throw new CustomerException(ClientConstants.CLIENT_LOANOFFICER_NOT_ASSIGNED);
            }

            if (client.getCustomerMeeting() == null || client.getCustomerMeeting().getMeeting() == null) {
                throw new CustomerException(GroupConstants.MEETING_NOT_ASSIGNED);
            }
        }
    }

    @Override
    public final GroupBO transferGroupTo(GroupBO group, CenterBO receivingCenter) throws CustomerException {

        group.validateNewCenter(receivingCenter);
        group.validateForActiveAccounts();

        setInitialObjectForAuditLogging(group);

        OfficeBO centerOffice = receivingCenter.getOffice();
        if (group.isDifferentBranch(centerOffice)) {
            group.makeCustomerMovementEntries(centerOffice);
            if (group.isActive()) {
                group.setCustomerStatus(new CustomerStatusEntity(CustomerStatus.GROUP_HOLD));
            }
        }

        CustomerBO oldParent = group.getParentCustomer();
        group.setParentCustomer(receivingCenter);

        CustomerHierarchyEntity currentHierarchy = group.getActiveCustomerHierarchy();
        if (null != currentHierarchy) {
            currentHierarchy.makeInactive(group.getUserContext().getId());
        }
        group.addCustomerHierarchy(new CustomerHierarchyEntity(group, receivingCenter));

        // handle parent
        group.setPersonnel(receivingCenter.getPersonnel());

        MeetingBO centerMeeting = receivingCenter.getCustomerMeetingValue();
        MeetingBO groupMeeting = group.getCustomerMeetingValue();
        if (centerMeeting != null) {
            if (groupMeeting != null) {
                if (!groupMeeting.getMeetingId().equals(centerMeeting.getMeetingId())) {
                    // FIXME - #000002 - this will store meeting waiting for batch job to come along and update schedules to match updated meeting frequency
                    group.setUpdatedMeeting(centerMeeting);
                }
            } else {
                CustomerMeetingEntity customerMeeting = group.createCustomerMeeting(centerMeeting);
                group.setCustomerMeeting(customerMeeting);
            }
        } else if (groupMeeting != null) {
            group.setCustomerMeeting(null);
        }

        if (oldParent != null) {
            oldParent.setUserContext(group.getUserContext());
        }

        receivingCenter.incrementChildCount();
        group.setSearchId(receivingCenter.getSearchId() + "." + String.valueOf(receivingCenter.getMaxChildCount()));

        receivingCenter.setUserContext(group.getUserContext());

        try {
            StaticHibernateUtil.startTransaction();

            group.setUpdateDetails();

            if (oldParent != null) {
                customerDao.save(oldParent);
            }
            customerDao.save(receivingCenter);

            customerDao.save(group);

            Set<CustomerBO> clients = group.getChildren();

            if (clients != null) {
                for (CustomerBO client : clients) {
                    client.setUserContext(group.getUserContext());
                    ((ClientBO) client).handleGroupTransfer();
                    client.setUpdateDetails();
                    customerDao.save(client);
                }
            }
            StaticHibernateUtil.commitTransaction();

            return group;
        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            StaticHibernateUtil.closeSession();
        }

    }

    @Override
    public final GroupBO transferGroupTo(GroupBO group, OfficeBO transferToOffice) throws CustomerException {

        group.validateNewOffice(transferToOffice);
        group.validateForActiveAccounts();

        customerDao.validateGroupNameIsNotTakenForOffice(group.getDisplayName(), transferToOffice.getOfficeId());

        if (group.isActive()) {
            group.setCustomerStatus(new CustomerStatusEntity(CustomerStatus.GROUP_HOLD));
        }

        group.makeCustomerMovementEntries(transferToOffice);
        group.setPersonnel(null);

        CustomerBO oldParentOfGroup = group.getParentCustomer();

        String searchId = null;
        if (oldParentOfGroup != null) {
            oldParentOfGroup.incrementChildCount();
            searchId = oldParentOfGroup.getSearchId() + "." + oldParentOfGroup.getMaxChildCount();
        } else {
            try {
                int newSearchIdSuffix = new CustomerPersistence().getMaxSearchIdSuffix(CustomerLevel.GROUP, group.getOffice().getOfficeId()) + 1;
                searchId = GroupConstants.PREFIX_SEARCH_STRING + newSearchIdSuffix;
            } catch (PersistenceException pe) {
                throw new CustomerException(pe);
            }
        }
        group.setSearchId(searchId);

        try {
            StaticHibernateUtil.startTransaction();

            group.setUpdateDetails();

            if (oldParentOfGroup != null) {
                customerDao.save(oldParentOfGroup);
            }

            customerDao.save(group);

            Set<CustomerBO> clients = group.getChildren();

            if (clients != null) {
                for (CustomerBO client : clients) {
                    client.setUserContext(group.getUserContext());
                    ((ClientBO) client).handleGroupTransfer();
                    client.setUpdateDetails();
                    customerDao.save(client);
                }
            }
            StaticHibernateUtil.commitTransaction();

            return group;
        } catch (Exception e) {
            StaticHibernateUtil.rollbackTransaction();
            throw new MifosRuntimeException(e);
        } finally {
            StaticHibernateUtil.closeSession();
        }
    }

    private void setInitialObjectForAuditLogging(Object object) {
        StaticHibernateUtil.getSessionTL();
        StaticHibernateUtil.getInterceptor().createInitialValueMap(object);
    }
}