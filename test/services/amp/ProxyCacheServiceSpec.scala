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

package services.amp

import java.time.{LocalDate, LocalDateTime}

import models.amp.Amp
import models.eab.Eab
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import services.ProxyCacheService
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

class ProxyCacheServiceSpec extends AmlsSpec with MockitoSugar with ScalaFutures with IntegrationPatience {

  val dateVal = LocalDateTime.now

  // AMP
  val completeAmpData = Json.obj(
    "typeOfParticipant"            -> Seq("artGalleryOwner"),
    "soldOverThreshold"            -> true,
    "dateTransactionOverThreshold" -> LocalDate.now,
    "identifyLinkedTransactions"   -> true,
    "percentageExpectedTurnover"   -> "fortyOneToSixty"
  )

  val completeAmpJson = Json.obj(
    "data"        -> completeAmpData,
    "hasChanged"  -> false,
    "hasAccepted" -> false
  )

  val completeAmpModel = Amp(completeAmpData)

  // EAB
  val completeEstateAgencyActPenalty = Json.obj(
    "penalisedEstateAgentsAct"       -> true,
    "penalisedEstateAgentsActDetail" -> "details"
  )

  val completePenalisedProfessionalBody = Json.obj(
    "penalisedProfessionalBody"       -> true,
    "penalisedProfessionalBodyDetail" -> "details"
  )

  val completeRedressScheme = Json.obj(
    "redressScheme"       -> "propertyRedressScheme",
    "redressSchemeDetail" -> "null"
  )

  val completeMoneyProtectionScheme = Json.obj(
    "clientMoneyProtectionScheme" -> true
  )

  val completeServiceList = Seq(
    "assetManagement",
    "auctioneering",
    "businessTransfer",
    "commercial",
    "developmentCompany",
    "landManagement",
    "lettings",
    "relocation",
    "residential",
    "socialHousingProvision"
  )

  val completeServices = Json.obj("eabServicesProvided" -> completeServiceList)

  val completeEabData = completeServices ++
    completeEstateAgencyActPenalty ++
    completePenalisedProfessionalBody ++
    completeRedressScheme ++
    completeMoneyProtectionScheme

  val completeEabJson = Json.obj(
    "data"        -> completeEabData,
    "hasChanged"  -> false,
    "hasAccepted" -> false
  )

  val completeEabModel = Eab(completeEabData)

  val credId = "someId"

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)
    val svc     = new ProxyCacheService(mockCacheConnector)
  }

  "AMP" when {
    "get" when {
      "cache data exists" when {
        "returns the amp section" in new Fixture {
          mockCacheFetch[Amp](Some(completeAmpModel), Some(Amp.key))

          whenReady(svc.getAmp(credId)) { result =>
            result mustBe Some(completeAmpJson)
          }
        }
      }

      "cache data does not exist" when {
        "returns null" in new Fixture {
          mockCacheFetch[Amp](None, Some(Amp.key))
          whenReady(svc.getAmp(credId)) { result =>
            result mustBe None
          }
        }
      }
    }

    "setAmp" when {
      "given valid json" when {
        "updates an existing model" in new Fixture {
          mockCacheFetch[Amp](Some(completeAmpModel), Some(Amp.key))
          mockCacheSave[Amp]

          whenReady(svc.setAmp(credId, completeAmpJson)) { result =>
            result mustBe mockCacheMap
          }
        }

        "saves a new model" in new Fixture {
          mockCacheFetch[Amp](None, Some(Amp.key))
          mockCacheSave[Amp]

          whenReady(svc.setAmp(credId, completeAmpJson)) { result =>
            result mustBe mockCacheMap
          }
        }
      }
    }
  }

  "EAB" when {
    "get" when {
      "cache data exists" when {
        "returns the eab section" in new Fixture {
          mockCacheFetch[Eab](Some(completeEabModel), Some(Eab.key))

          whenReady(svc.getEab(credId)) { result =>
            result mustBe Some(completeEabJson)
          }
        }
      }

      "cache data does not exist" when {
        "returns null" in new Fixture {
          mockCacheFetch[Eab](None, Some(Eab.key))
          whenReady(svc.getEab(credId)) { result =>
            result mustBe None
          }
        }
      }
    }

    "setEab" when {
      "given valid json" when {
        "updates an existing model" in new Fixture {
          mockCacheFetch[Eab](Some(completeEabModel), Some(Eab.key))
          mockCacheSave[Eab]

          whenReady(svc.setEab(credId, completeEabJson)) { result =>
            result mustBe mockCacheMap
          }
        }

        "saves a new model" in new Fixture {
          mockCacheFetch[Eab](None, Some(Eab.key))
          mockCacheSave[Eab]

          whenReady(svc.setEab(credId, completeEabJson)) { result =>
            result mustBe mockCacheMap
          }
        }
      }
    }
  }
}
