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

package views.businessdetails

import javax.inject.Inject
import views.html.businessdetails._

class BusinessDetails @Inject()(
                               activity_start_date: activity_start_date,
                               confirm_registered_office_or_main_place: confirm_registered_office_or_main_place,
                               contacting_you: contacting_you,
                               contacting_you_phone: contacting_you_phone,
                               correspondence_address_is_uk: correspondence_address_is_uk,
                               correspondence_address_non_uk: correspondence_address_non_uk,
                               correspondence_address_uk: correspondence_address_uk,
                               letters_address: letters_address,
                               previously_registered: previously_registered,
                               registered_office_is_uk: registered_office_is_uk,
                               registered_office_non_uk: registered_office_non_uk,
                               registered_office_uk: registered_office_uk,
                               summary: summary,
                               vat_registered: vat_registered,
                               what_you_need: what_you_need
                               ) {

}
