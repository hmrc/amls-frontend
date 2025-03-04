/*
 * Copyright 2024 HM Revenue & Customs
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

package services.msb

import connectors.DataCacheConnector
import models.Country
import models.moneyservicebusiness.{BranchesOrAgents, BranchesOrAgentsHasCountries, BranchesOrAgentsWhichCountries, MoneyServiceBusiness}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Results.Redirect
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class BranchesOrAgentsWhichCountriesServiceSpec extends AmlsSpec with BeforeAndAfterEach {

  val mockCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val service                                = new BranchesOrAgentsWhichCountriesService(mockCacheConnector)

  val credId = "1234567890"

  override def beforeEach(): Unit = {
    reset(mockCacheConnector)
    super.beforeEach()
  }

  "BranchesOrAgentsWhichCountriesService" when {

    ".fetchBranchesOrAgents is called" must {

      "return an object" when {

        "data cache hold a valid record" in {

          val obj = BranchesOrAgentsWhichCountries(Seq(Country("United States", "US")))

          when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any())) thenReturn Future.successful(
            Some(
              MoneyServiceBusiness(
                branchesOrAgents = Some(
                  BranchesOrAgents(
                    BranchesOrAgentsHasCountries(true),
                    Some(obj)
                  )
                )
              )
            )
          )

          service.fetchBranchesOrAgents(credId).futureValue mustBe Some(obj)
        }
      }

      "return None" when {

        "MSB record is present but branches or agents is empty" in {

          when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any())) thenReturn Future.successful(
            Some(MoneyServiceBusiness())
          )

          service.fetchBranchesOrAgents(credId).futureValue mustBe None
        }

        "no record is present" in {

          when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any())) thenReturn Future.successful(None)

          service.fetchBranchesOrAgents(credId).futureValue mustBe None
        }
      }
    }

    ".fetchAndSaveBranchesOrAgents is called" must {

      "return the given redirect after saving data" in {
        val msb        = MoneyServiceBusiness()
        val data       = BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB")))
        val updatedMsb = msb.branchesOrAgents(
          BranchesOrAgents.update(
            msb.branchesOrAgents.getOrElse(BranchesOrAgents(BranchesOrAgentsHasCountries(false), None)),
            data
          )
        )

        when(mockCacheConnector.fetch[MoneyServiceBusiness](meq(credId), any())(any())) thenReturn Future.successful(
          Some(MoneyServiceBusiness())
        )

        when(
          mockCacheConnector.save[MoneyServiceBusiness](meq(credId), any(), meq(updatedMsb))(any())
        ) thenReturn Future.successful(Cache("id", Map.empty))

        val redirect = Redirect("/foo")
        service.fetchAndSaveBranchesOrAgents(credId, data, redirect).futureValue mustBe redirect

        verify(mockCacheConnector).fetch(meq(credId), any())(any())
        verify(mockCacheConnector).save(meq(credId), any(), meq(updatedMsb))(any())
      }
    }
  }
}
