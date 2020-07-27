/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.responsiblepeople

import javax.inject.Inject
import views.html.responsiblepeople.address._
import views.html.responsiblepeople._

class ResponsiblePeople @Inject()(
                                 additional_address: additional_address,
                                 additional_address_NonUK: additional_address_NonUK,
                                 additional_address_UK: additional_address_UK,
                                 additional_extra_address: additional_extra_address,
                                 additional_extra_address_NonUK: additional_extra_address_NonUK,
                                 additional_extra_address_UK: additional_extra_address_UK,
                                 current_address: current_address,
                                 current_address_NonUK: current_address_NonUK,
                                 current_address_UK: current_address_UK,
                                 moved_address: moved_address,
                                 new_home_address: new_home_address,
                                 new_home_address_NonUK: new_home_address_NonUK,
                                 new_home_address_UK: new_home_address_UK,
                                 new_home_date_of_change: new_home_date_of_change,
                                 time_at_additional_address: time_at_additional_address,
                                 time_at_additional_extra_address: time_at_additional_extra_address,
                                 time_at_address: time_at_address,
                                 approval_check: approval_check,
                                 contact_details: contact_details,
                                 country_of_birth: country_of_birth,
                                 date_of_birth: date_of_birth,
                                 detailed_answers: detailed_answers,
                                 experience_training: experience_training,
                                 fit_and_proper: fit_and_proper,
                                 fit_and_proper_notice: fit_and_proper_notice,
                                 known_by: known_by,
                                 legal_name: legal_name,
                                 legal_name_change_date: legal_name_change_date,
                                 legal_name_input: legal_name_input,
                                 nationality: nationality,
                                 person_name: person_name,
                                 person_non_uk_passport: person_non_uk_passport,
                                 person_residence_type: person_residence_type,
                                 person_uk_passport: person_uk_passport,
                                 position_within_business: position_within_business,
                                 position_within_business_start_date: position_within_business_start_date,
                                 registered_for_self_assessment: registered_for_self_assessment,
                                 remove_responsible_person: remove_responsible_person,
                                 sole_proprietor: sole_proprietor,
                                 training: training,
                                 vat_registered: vat_registered,
                                 what_you_need: what_you_need,
                                 who_must_register: who_must_register,
                                 your_responsible_people: your_responsible_people
                                 ) {

}
