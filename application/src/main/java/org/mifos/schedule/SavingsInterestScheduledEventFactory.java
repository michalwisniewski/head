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

package org.mifos.schedule;

import org.mifos.application.meeting.business.MeetingBO;
import org.mifos.schedule.internal.DailyScheduledEvent;
import org.mifos.schedule.internal.MonthlyAtEndOfMonthScheduledEvent;

public class SavingsInterestScheduledEventFactory {

    public static ScheduledEvent createScheduledEventFrom(final MeetingBO meeting) {

        ScheduledEvent scheduledEvent = new DailyScheduledEvent(meeting.getRecurAfter());
        switch (meeting.getMeetingTypeEnum()) {
        case SAVINGS_INTEREST_CALCULATION_TIME_PERIOD:
            if (meeting.isMonthly()) {
                scheduledEvent = new MonthlyAtEndOfMonthScheduledEvent(meeting.getRecurAfter());
            }
            break;
        case SAVINGS_INTEREST_POSTING:
            scheduledEvent = new MonthlyAtEndOfMonthScheduledEvent(meeting.getRecurAfter());
            break;
        default:
            break;
        }

        return scheduledEvent;
    }
}