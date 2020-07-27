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

package views.businessmatching

import javax.inject.Inject
import views.html.businessmatching._
import views.html.businessmatching.updateservice.add._
import views.html.businessmatching.updateservice.change_services
import views.html.businessmatching.updateservice.remove._

class BusinessMatching @Inject()(
                                add_more_activities: add_more_activities,
                                business_applied_for_psr_number: business_applied_for_psr_number,
                                cannot_add_services: views.html.businessmatching.updateservice.add.cannot_add_services,
                                msb_subservices: msb_subservices,
                                new_service_information: new_service_information,
                                select_activities: select_activities,
                                update_services_summary: update_services_summary,
                                need_more_information: need_more_information,
                                remove_activities: remove_activities,
                                remove_activities_information: remove_activities_information,
                                remove_activities_summary: remove_activities_summary,
                                unable_to_remove_activity: unable_to_remove_activity,
                                change_services: change_services,
                                business_type: business_type,
                                cannotAddServices: views.html.businessmatching.cannot_add_services,
                                cannot_continue_with_the_application: cannot_continue_with_the_application,
                                company_registration_number: company_registration_number,
                                confirm_postcode: confirm_postcode,
                                psr_number: psr_number,
                                register_services: register_services,
                                services: services,
                                summary: summary,
                                type_of_business: type_of_business
                                ) {

}
