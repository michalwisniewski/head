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

package org.mifos.customers.persistence;

import org.joda.time.DateTime;
import org.mifos.accounts.business.AccountBO;
import org.mifos.accounts.fees.business.FeeBO;
import org.mifos.accounts.loan.business.LoanBO;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.master.business.ValueListElement;
import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.application.questionnaire.migration.CustomFieldForMigrationDto;
import org.mifos.customers.business.CustomerBO;
import org.mifos.accounts.api.CustomerDto;
import org.mifos.customers.business.CustomerFlagDetailEntity;
import org.mifos.customers.business.CustomerMeetingEntity;
import org.mifos.customers.business.CustomerPerformanceHistoryDto;
import org.mifos.customers.business.CustomerStatusEntity;
import org.mifos.customers.center.business.CenterBO;
import org.mifos.customers.client.business.ClientBO;
import org.mifos.customers.exceptions.CustomerException;
import org.mifos.customers.group.business.GroupBO;
import org.mifos.customers.personnel.business.PersonnelBO;
import org.mifos.customers.personnel.business.PersonnelDto;
import org.mifos.customers.personnel.business.PersonnelLevelEntity;
import org.mifos.customers.util.helpers.CenterDisplayDto;
import org.mifos.customers.util.helpers.CenterPerformanceHistoryDto;
import org.mifos.customers.util.helpers.ClientDisplayDto;
import org.mifos.customers.util.helpers.CustomerAccountSummaryDto;
import org.mifos.customers.util.helpers.CustomerAddressDto;
import org.mifos.customers.util.helpers.CustomerDetailDto;
import org.mifos.customers.util.helpers.CustomerFlagDto;
import org.mifos.customers.util.helpers.CustomerMeetingDto;
import org.mifos.customers.util.helpers.CustomerNoteDto;
import org.mifos.customers.util.helpers.CustomerPositionDto;
import org.mifos.customers.util.helpers.GroupDisplayDto;
import org.mifos.customers.util.helpers.LoanCycleCounter;
import org.mifos.customers.util.helpers.LoanDetailDto;
import org.mifos.customers.util.helpers.SavingsDetailDto;
import org.mifos.customers.util.helpers.SurveyDto;
import org.mifos.dto.domain.CustomFieldDto;
import org.mifos.framework.components.fieldConfiguration.business.FieldConfigurationEntity;
import org.mifos.framework.hibernate.helper.QueryResult;
import org.mifos.security.util.UserContext;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 *
 */
public interface CustomerDao {

    void save(CustomerBO customer);

    void save(AccountBO customerAccount);

    void save(CustomerStatusEntity cse);

    CustomerBO findCustomerById(Integer customerId);

    ClientBO findClientBySystemId(String globalCustNum);

    GroupBO findGroupBySystemId(String globalCustNum);

    CenterBO findCenterBySystemId(String globalCustNum);

    List<ClientBO> findActiveClientsUnderGroup(CustomerBO customer);

    List<CustomFieldDto> retrieveCustomFieldsForCenter(UserContext userContext);

    List<CustomFieldDto> retrieveCustomFieldsForPersonnel(Locale preferredLocale);

    List<CustomFieldDefinitionEntity> retrieveCustomFieldEntitiesForPersonnel();

    List<CustomFieldDefinitionEntity> retrieveCustomFieldEntitiesForCenter();

    List<CustomFieldDefinitionEntity> retrieveCustomFieldEntitiesForGroup();

    List<CustomFieldDefinitionEntity> retrieveCustomFieldEntitiesForClient();

    List<FeeBO> retrieveFeesApplicableToCenters();

    List<FeeBO> retrieveFeesApplicableToGroups();

    List<FeeBO> retrieveFeesApplicableToClients();

    List<FeeBO> retrieveFeesApplicableToGroupsRefinedBy(MeetingBO customerMeeting);

    List<FeeBO> retrieveFeesApplicableToClientsRefinedBy(MeetingBO customerMeetingValue);

    List<CustomerDetailDto> findClientsThatAreNotCancelledOrClosedReturningDetailDto(String searchId, Short branchId);

    List<CustomerDto> findClientsThatAreNotCancelledOrClosed(String parentSearchId, Short parentOfficeId);

    List<CustomerDto> findGroupsThatAreNotCancelledOrClosed(String parentSearchId, Short parentOfficeId);

    QueryResult search(String normalisedSearchString, PersonnelBO user);

    List<ValueListElement> retrieveSalutations();

    List<ValueListElement> retrieveTitles();

    List<ValueListElement> retrieveMaritalStatuses();

    List<ValueListElement> retrieveCitizenship();

    List<ValueListElement> retrieveBusinessActivities();

    List<ValueListElement> retrieveEducationLevels();

    List<ValueListElement> retrieveGenders();

    List<ValueListElement> retrieveHandicapped();

    List<ValueListElement> retrieveEthinicity();

    List<ValueListElement> retrievePoverty();

    List<ValueListElement> retrieveLivingStatus();

    List<ValueListElement> retrieveLanguages();

    CenterDisplayDto getCenterDisplayDto(Integer centerId, UserContext userContext);

    CustomerAccountSummaryDto getCustomerAccountSummaryDto(Integer centerId);

    CustomerAddressDto getCustomerAddressDto(CustomerBO center);

    List<CustomerDetailDto> getGroupsOtherThanClosedAndCancelledForGroup(String searchId, Short branchId);

    List<CustomerNoteDto> getRecentCustomerNoteDto(Integer centerId);

    List<CustomerPositionDto> getCustomerPositionDto(Integer centerId, UserContext userContext);

    CustomerMeetingDto getCustomerMeetingDto(CustomerMeetingEntity customerMeeting, UserContext userContext);

    List<SurveyDto> getCustomerSurveyDto(Integer centerId);

    List<CustomFieldDto> getCustomFieldViewForCustomers(Integer centerId, Short value, UserContext userContext);

    CenterPerformanceHistoryDto getCenterPerformanceHistory(String searchId, Short branchId);

    Integer getActiveAndOnHoldClientCountForGroup(String searchId, Short branchId);

    List<CustomerDetailDto> findActiveCentersUnderUser(PersonnelBO personnel);

    List<CustomerDetailDto> findGroupsUnderUser(PersonnelBO personnel);

    CustomerStatusEntity findClientPendingStatus();

    CustomerStatusEntity findGroupPendingStatus();

    int countOfClients();

    int countOfGroups();

    int retrieveLastSearchIdValueForNonParentCustomersInOffice(Short officeIdValue);

    List<FieldConfigurationEntity> findMandatoryConfigurableFieldsApplicableToCenter();

    void updateLoanOfficersForAllChildrenAndAccounts(Short loanOfficerId, String searchId, Short officeId);

    boolean validateGovernmentIdForClient(String governmentId);

    void validateClientForDuplicateNameOrGovtId(ClientBO client) throws CustomerException;

    void validateGroupNameIsNotTakenForOffice(String displayName, Short officeId) throws CustomerException;

    void validateCenterNameIsNotTakenForOffice(String displayName, Short officeId) throws CustomerException;

    void checkPermissionForStatusChange(Short value, UserContext userContext, Short statusFlagId, Short officeId, Short personnelId) throws CustomerException;

    void checkPermissionForEditMeetingSchedule(UserContext userContext, CustomerBO customer) throws CustomerException;

    boolean validateForClosedClientsOnNameAndDob(final String name, final DateTime dateOfBirth);

    boolean validateForBlackListedClientsOnNameAndDob(String clientName, DateTime dateOfBirth);

    // FIXME - #000003 - keithw - inspect below methods to check are they non customer related methods to be moved out to other DAOs
    List<SavingsDetailDto> getSavingsDetailDto(Integer centerId, UserContext userContext);

    List<SavingsDetailDto> retrieveSavingOfferingsApplicableToClient();

    List<PersonnelDto> findLoanOfficerThatFormedOffice(Short officeId);

    String getAvgLoanAmountForMemberInGoodOrBadStanding(String searchId, Short branchId);

    String getTotalLoanAmountForGroup(String groupSearchId, Short groupOfficeId);

    String getTotalOutstandingLoanAmountForGroupAndClientsOfGroups(String searchId, Short branchId);

    String getTotalSavingsAmountForGroupandClientsOfGroup(String searchId, Short branchId);

    List<LoanCycleCounter> fetchLoanCycleCounter(Integer groupId, Short value);

    GroupDisplayDto getGroupDisplayDto(Integer customerId, UserContext userContext);

    ClientDisplayDto getClientDisplayDto(Integer customerId, UserContext userContext);

    List<CustomerFlagDto> getCustomerFlagDto(Set<CustomerFlagDetailEntity> customerFlags);

    List<LoanDetailDto> getLoanDetailDto(List<LoanBO> openLoanAccounts);

    CustomerPerformanceHistoryDto numberOfMeetings(boolean bool, Integer clientId);

    List<AccountBO> findGLIMLoanAccountsApplicableTo(Integer customerId, Integer customerWithActiveAccount);

    void checkPermissionForDefaultFeeRemoval(UserContext userContext, Short officeId, Short personnelId) throws CustomerException;

    void checkPermissionForDefaultFeeRemovalFromLoan(UserContext userContext, CustomerBO customer) throws CustomerException;

    List<PersonnelLevelEntity> retrievePersonnelLevels();

    List<Object[]> getCustomFieldResponses(List<Short> customFieldIds);

    /**
     * <code>phoneNumber</code> is stripped to contain numeric characters only
     */
    List<CustomerDto> findCustomersWithGivenPhoneNumber(String phoneNumber);
}