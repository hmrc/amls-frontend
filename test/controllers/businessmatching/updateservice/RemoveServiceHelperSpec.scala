/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.businessmatching.updateservice

import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessActivitiesGenerator
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.flowmanagement.RemoveServiceFlowModel
import org.scalatest.MustMatchers
import services.{ResponsiblePeopleService, TradingPremisesService}
import utils.{AuthorisedFixture, DependencyMocks, FutureAssertions, GenericTestHelper}

//noinspection ScalaStyle
class RemoveServiceHelperSpec extends GenericTestHelper
  with MustMatchers
  with BusinessActivitiesGenerator
  with ResponsiblePersonGenerator
  with FutureAssertions {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val tradingPremisesService = mock[TradingPremisesService]
    val mockUpdateServiceHelper = mock[UpdateServiceHelper]
    val responsiblePeopleService = mock[ResponsiblePeopleService]

    val helper = new RemoveServiceHelper(
      self.authConnector,
      mockCacheConnector
    )

//    val businessActivitiesSection = BusinessActivities(
//      involvedInOther = Some(InvolvedInOtherNo),
//      whoIsYourAccountant = Some(mock[WhoIsYourAccountant]),
//      accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
//      taxMatters = Some(TaxMatters(true)),
//      hasAccepted = true
//    )
  }

  "removing MSB activity" must {

    "TCSP exists already" when {

      "the BusinessMatching Activity must be removed" in new Fixture {
        val model = RemoveServiceFlowModel( existingActivities = Some(Set(HighValueDealing, MoneyServiceBusiness)),
                                            activitiesToRemove = Some(Set(MoneyServiceBusiness)))

        val startResultMatching = BusinessMatching( activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))),
                                                    hasAccepted = true,
                                                    hasChanged = true)

        var endResultMatching = BusinessMatching( activities = Some(BMBusinessActivities(Set(HighValueDealing))),
                                                  hasAccepted = true,
                                                  hasChanged = true)

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))))),
          Some(BusinessMatching.key))

        mockCacheUpdate(Some(BusinessMatching.key), startResultMatching )

        helper.removeBusinessActivities(model).returnsSome(endResultMatching)
      }

      "the BusinessMatching MSB services be removed" in {

      }

      "the BusinessMatching PSR must be removed" in {

      }

      "the TradingPremises Business.Activity must be removed" in {

      }

      "the TradingPremises MSBServices must be removed" in {

      }

      "the MSB Section Data must be removed" in {

      }

      "FitAndProper must NOT be removed" in {

      }

      "All the appropriate data has been removed" in {

      }

    }

  }


}
