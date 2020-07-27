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

package views.tcsp

import javax.inject.Inject
import views.html.tcsp._

class Tcsp @Inject()(
                    another_tcsp_supervision: another_tcsp_supervision,
                    complex_corp_structure_creation: complex_corp_structure_creation,
                    only_off_the_shelf_compos_sold: only_off_the_shelf_comps_sold,
                    provided_services: provided_services,
                    service_provider_types: service_provider_types,
                    service_of_another_tcsp: services_of_another_tcsp,
                    summary: summary,
                    what_you_need: what_you_need
                    ) {

}
