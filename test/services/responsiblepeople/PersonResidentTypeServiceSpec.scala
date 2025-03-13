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

package services.responsiblepeople

import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.{Enrolments, User}
import uk.gov.hmrc.domain.Nino
import services.cache.Cache
import utils.{AmlsSpec, AuthorisedRequest}

import scala.concurrent.Future

class PersonResidentTypeServiceSpec extends AmlsSpec with ResponsiblePeopleValues with BeforeAndAfterEach {

  lazy val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  lazy val mockCacheMap: Cache                        = mock[Cache]
  lazy val service                                    = new PersonResidentTypeService(mockDataCacheConnector)

  implicit val authorisedRequest: AuthorisedRequest[AnyContentAsEmpty.type] = AuthorisedRequest(
    FakeRequest(),
    Some("REF"),
    "CREDID",
    Organisation,
    Enrolments(Set()),
    ("TYPE", "ID"),
    Some("GROUPID"),
    Some(User)
  )

  override def beforeEach(): Unit = {
    reset(mockDataCacheConnector, mockCacheMap)
    super.beforeEach()
  }

  "PersonResidentTypeService" when {

    ".getCache is called" when {

      "Responsible person is a UK resident" must {

        "retrieve, update and save the model to the cache correctly" in {

          val answer = PersonResidenceType(UKResidence(Nino(nextNino)), None, None)

          when(mockDataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any())) thenReturn Some(Seq(ResponsiblePerson()))

          when(mockDataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])

          val result = service.getCache(answer, authorisedRequest.credId, 1)

          result.value.futureValue

          verify(mockDataCacheConnector).save[Seq[ResponsiblePerson]](any(), any(), captor.capture())(any())

          captor.getValue.head.personResidenceType.value mustBe answer
        }
      }

      "Responsible person is NOT a UK resident" must {

        "retrieve, update and save the model to the cache correctly" in {

          val answer = PersonResidenceType(
            NonUKResidence,
            Some(Country("United States", "US")),
            Some(Country("Spain", "ES"))
          )

          when(mockDataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any())) thenReturn Some(Seq(ResponsiblePerson()))

          when(mockDataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val captor = ArgumentCaptor.forClass(classOf[Seq[ResponsiblePerson]])

          val result = service.getCache(answer, authorisedRequest.credId, 1)

          result.value.futureValue

          verify(mockDataCacheConnector).save[Seq[ResponsiblePerson]](any(), any(), captor.capture())(any())

          captor.getValue.head.personResidenceType.value mustBe PersonResidenceType(NonUKResidence, None, None)
        }
      }

      "no Responsible person is found" must {

        "not update or save the model" in {

          val answer = PersonResidenceType(NonUKResidence, None, None)

          when(mockDataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any())) thenReturn None

          verify(mockDataCacheConnector, times(0))
            .save[Seq[ResponsiblePerson]](any(), any(), any())(any())

          service.getCache(answer, authorisedRequest.credId, 1).value.futureValue mustBe Some(mockCacheMap)
        }
      }

      "no Cache Map is found" must {

        "not update or save the model" in {

          val answer = PersonResidenceType(NonUKResidence, None, None)

          when(mockDataCacheConnector.fetchAll(any())).thenReturn(Future.successful(None))

          verifyNoInteractions(mockCacheMap)

          verify(mockDataCacheConnector, times(0))
            .save[Seq[ResponsiblePerson]](any(), any(), any())(any())

          service.getCache(answer, authorisedRequest.credId, 1).value.futureValue mustBe None
        }
      }
    }

    ".getResponsiblePerson is called" must {

      "return a Responsible Person" when {

        "there is a responsible person in the cache" in {

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(completeResponsiblePerson))))

          service.getResponsiblePerson("1234567890", 1).futureValue mustBe Some(completeResponsiblePerson)
        }
      }

      "return None" when {

        "there is no responsible person in the cache" in {

          when(mockDataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(None))

          service.getResponsiblePerson("1234567890", 1).futureValue mustBe None
        }
      }
    }
  }
}
