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

import cats.data.OptionT
import cats.implicits._
import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessActivitiesGenerator
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.flowmanagement.RemoveServiceFlowModel
import org.scalatest.MustMatchers
import services.{ResponsiblePeopleService, TradingPremisesService}
import utils.{AuthorisedFixture, DependencyMocks, FutureAssertions, GenericTestHelper}

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

//noinspection ScalaStyle
class RemoveServiceHelperSpec extends GenericTestHelper
  with MustMatchers
  with BusinessActivitiesGenerator
  with ResponsiblePersonGenerator
  with FutureAssertions {


  val MSBOnlyModel = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness)))

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val SUT = new RemoveServiceHelper(
      self.authConnector,
      mockCacheConnector
    )
  }

  "The removeBusinessMatchingBusinessActivities method" must {

    "remove the BusinessMatching Business Activity MSB (Type) " when {
      "MSB is not the only Activity in the list of existing activities" in new Fixture {
        val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness)))

        val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))),
          hasAccepted = true,
          hasChanged = true)

        var endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing))),
          hasAccepted = true,
          hasChanged = true)

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))))),
          Some(BusinessMatching.key))

        mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)

        SUT.removeBusinessMatchingBusinessActivities(model).returnsSome(endResultMatching)
      }
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

    "All the appropriate data has been removed" in {

    }


  }


}
