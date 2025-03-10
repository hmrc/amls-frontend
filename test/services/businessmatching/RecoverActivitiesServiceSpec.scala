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

package services.businessmatching

import connectors.{AmlsConnector, DataCacheConnector}
import models.ViewResponse
import models.businessmatching.BusinessActivity.ArtMarketParticipant
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.{Enrolments, User}
import services.cache.Cache
import utils.{AmlsSpec, AuthorisedRequest}

import scala.concurrent.Future

class RecoverActivitiesServiceSpec extends AmlsSpec with MockitoSugar {

  val mockAmlsConnector: AmlsConnector                   = mock[AmlsConnector]
  val mockDataCacheConnector: DataCacheConnector         = mock[DataCacheConnector]
  val mockCacheMap: Cache                                = mock[Cache]
  val service                                            = new RecoverActivitiesService(mockAmlsConnector, mockDataCacheConnector)
  val emptyBusinessMatching: BusinessMatching            = BusinessMatching()
  val emptyBusinessActivities: BusinessActivities        = BusinessActivities(Set())
  val viewResponse: ViewResponse                         = ViewResponse(
    etmpFormBundleNumber = "FORMBUNDLENUMBER",
    businessMatchingSection = emptyBusinessMatching,
    eabSection = None,
    tradingPremisesSection = None,
    businessDetailsSection = None,
    bankDetailsSection = Seq(None),
    aboutYouSection = AddPerson(
      "FirstName",
      None,
      "LastName",
      RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))
    ),
    businessActivitiesSection = None,
    responsiblePeopleSection = None,
    tcspSection = None,
    aspSection = None,
    msbSection = None,
    hvdSection = None,
    ampSection = None,
    supervisionSection = None
  )
  val request: AuthorisedRequest[AnyContentAsEmpty.type] = AuthorisedRequest(
    addToken(authRequest),
    Some("REF"),
    "CREDID",
    Individual,
    Enrolments(Set()),
    ("TYPE", "ID"),
    Some("GROUPID"),
    Some(User)
  )

  ".recover" must {

    "return true if business types were successfully retrieved from DES and updated" in {
      val businessActivities = emptyBusinessActivities.copy(businessActivities = Set(ArtMarketParticipant))
      val businessMatching   = emptyBusinessMatching.copy(activities = Some(businessActivities))
      val desResponse        = viewResponse.copy(businessMatchingSection = businessMatching)

      when(mockDataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(emptyBusinessMatching)))

      when(mockAmlsConnector.view(any(), any())(any(), any(), any())).thenReturn(Future.successful(desResponse))

      when(mockDataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      service.recover(request).futureValue mustBe true
    }

    "return false if DES returns an empty list of business types" in {
      val businessMatching = emptyBusinessMatching.copy(activities = Some(emptyBusinessActivities))
      val desResponse      = viewResponse.copy(businessMatchingSection = businessMatching)

      when(mockDataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(emptyBusinessMatching)))

      when(mockAmlsConnector.view(any(), any())(any(), any(), any())).thenReturn(Future.successful(desResponse))

      when(mockDataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      service.recover(request).futureValue mustBe false
    }

    "return false if DES returns an empty BusinessActivities section" in {
      when(mockDataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(emptyBusinessMatching)))

      when(mockAmlsConnector.view(any(), any())(any(), any(), any())).thenReturn(Future.successful(viewResponse))

      when(mockDataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))

      service.recover(request).futureValue mustBe false
    }

    "return false is there is no AMLS ref number in the request" in {
      service.recover(request.copy(amlsRefNumber = None)).futureValue mustBe false
    }
  }
}
