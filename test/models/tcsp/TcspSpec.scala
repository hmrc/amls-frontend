/*
 * Copyright 2017 HM Revenue & Customs
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

package models.tcsp

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeApplication
import uk.gov.hmrc.http.cache.client.CacheMap


trait TcspValues {

  object DefaultValues {

    private val offTheShelf = true
    private val complexStructure = false

    val DefaultProvidedServices = ProvidedServices(Set(PhonecallHandling, Other("other service")))
    val DefaultCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider,
      TrusteeProvider,
      CompanyDirectorEtc,
      CompanyFormationAgent(offTheShelf, complexStructure)))
    val DefaultServicesOfAnotherTCSP = ServicesOfAnotherTCSPYes("12345678")

  }

  object NewValues {

    private val offTheShelf = true
    private val complexStructure = false

    val NewProvidedServices = ProvidedServices(Set(EmailHandling))
    val NewCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider,
      CompanyFormationAgent(offTheShelf, complexStructure)))
    val NewServicesOfAnotherTCSP = ServicesOfAnotherTCSPNo

  }

  val completeJson = Json.obj(
    "tcspTypes" -> Json.obj(
      "serviceProviders" -> Seq("01", "02", "04", "05"),
      "onlyOffTheShelfCompsSold" -> true,
      "complexCorpStructureCreation" -> false
    ),
    "providedServices" -> Json.obj(
      "services" -> Seq("01", "08"),
      "details" -> "other service"
    ),
    "servicesOfAnotherTCSP" -> Json.obj(
      "servicesOfAnotherTCSP" -> true,
      "mlrRefNumber" -> "12345678"
    ),
    "hasChanged" -> false,
    "hasAccepted" -> true
  )

  val completeModel = Tcsp(
    Some(DefaultValues.DefaultCompanyServiceProviders),
    Some(DefaultValues.DefaultProvidedServices),
    Some(DefaultValues.DefaultServicesOfAnotherTCSP),
    hasAccepted = true
  )
}

class TcspSpec extends PlaySpec with MockitoSugar with TcspValues with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.has-accepted" -> false))

  "Tcsp" must {

    "have a default function that" must {

      "correctly provide a default value when none is provided" in {
        Tcsp.default(None) must be(Tcsp())
      }

      "correctly provide a default value when existing value is provided" in {
        Tcsp.default(Some(completeModel)) must be(completeModel)
      }
    }

    "have a mongo key that" must {
      "be correctly set" in {
        Tcsp.mongoKey() must be("tcsp")
      }
    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {

        val notStartedSection = Section("tcsp", NotStarted, false, controllers.tcsp.routes.WhatYouNeedController.get())

        when(cache.getEntry[Tcsp]("tcsp")) thenReturn None

        Tcsp.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Tcsp]
        val completedSection = Section("tcsp", Completed, false, controllers.tcsp.routes.SummaryController.get())

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Tcsp]("tcsp")) thenReturn Some(complete)

        Tcsp.section must be(completedSection)

      }

      "return a Started Section when model is incomplete" in {

        val incompleteTcsp = mock[Tcsp]
        val startedSection = Section("tcsp", Started, false, controllers.tcsp.routes.WhatYouNeedController.get())

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Tcsp]("tcsp")) thenReturn Some(incompleteTcsp)

        Tcsp.section must be(startedSection)

      }
    }

    "Serialise as expected" in {
      Json.toJson(completeModel) must be(completeJson)
    }

    "Deserialise as expected" in {
      completeJson.as[Tcsp] must be(completeModel)
    }

    "None" when {
      val initial: Option[Tcsp] = None

      "Merged with Company Service Providers" must {
        "return Tcsp with correct Company Service Providers" in {
          val result = initial.tcspTypes(NewValues.NewCompanyServiceProviders)
          result must be(Tcsp(tcspTypes = Some(NewValues.NewCompanyServiceProviders), hasChanged = true))
        }
      }

      "Merged with Provided Services" must {
        "return Tcsp with correct Provided Services" in {
          val result = initial.providedServices(NewValues.NewProvidedServices)
          result must be(Tcsp(providedServices = Some(NewValues.NewProvidedServices), hasChanged = true))
        }
      }
      "Merged with services of another tcsp" must {
        "return Tcsp with correct services of another tcsp" in {
          val result = initial.servicesOfAnotherTCSP(NewValues.NewServicesOfAnotherTCSP)
          result must be(Tcsp(servicesOfAnotherTCSP = Some(NewValues.NewServicesOfAnotherTCSP), hasChanged = true))
        }
      }
    }
  }

  "isComplete" must {
    "return true if the model is complete" in {
      completeModel.isComplete must be(true)
    }
    val initial: Option[Tcsp] = None

    "return false if the model is incomplete" in {
      val incompleteModel = initial.copy(providedServices = None)
      incompleteModel.isComplete must be(false)
    }
  }

  "TCSP class" when {
    "tcspTypes value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.tcspTypes(DefaultValues.DefaultCompanyServiceProviders)
          res.hasChanged must be(false)
          res must be(completeModel)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.tcspTypes(NewValues.NewCompanyServiceProviders)
          res.hasChanged must be(true)
          res.tcspTypes must be(Some(NewValues.NewCompanyServiceProviders))
        }
      }
    }
    "providedServices value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.providedServices(DefaultValues.DefaultProvidedServices)
          res.hasChanged must be(false)
          res must be(completeModel)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.providedServices(NewValues.NewProvidedServices)
          res.hasChanged must be(true)
          res.providedServices must be(Some(NewValues.NewProvidedServices))
        }
      }
    }
    "servicesOfAnotherTCSP value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.servicesOfAnotherTCSP(DefaultValues.DefaultServicesOfAnotherTCSP)
          res.hasChanged must be(false)
          res must be(completeModel)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.servicesOfAnotherTCSP(NewValues.NewServicesOfAnotherTCSP)
          res.hasChanged must be(true)
          res.servicesOfAnotherTCSP must be(Some(NewValues.NewServicesOfAnotherTCSP))
        }
      }
    }
  }
}

class TcspWithHasAcceptedSpec extends PlaySpec with MockitoSugar with TcspValues with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.has-accepted" -> true))

  "Tcsp" must {

    "isComplete" must {
      "return true if the model is accepted" in {
        completeModel.copy(hasAccepted = true).isComplete must be(true)
      }
      val initial: Option[Tcsp] = None

      "return false if the model is accepted" in {
        completeModel.isComplete must be(false)
      }
    }

  }
}
