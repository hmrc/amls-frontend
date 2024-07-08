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

package models.tcsp

import models.registrationprogress._
import models.tcsp.ProvidedServices._
import models.tcsp.TcspTypes._
import org.mockito.Mockito._
import play.api.libs.json.Json
import services.cache.Cache
import utils.AmlsSpec

trait TcspValues {

  object DefaultValues {

    val DefaultProvidedServices = ProvidedServices(Set(PhonecallHandling, Other("other service")))

    val DefaultCompanyServiceProviders = TcspTypes(Set(
      NomineeShareholdersProvider,
      TrusteeProvider,
      CompanyDirectorEtc,
      CompanyFormationAgent))
    val DefaultServicesOfAnotherTCSP = ServicesOfAnotherTCSPYes(Some("12345678"))

    val DefaultCompanyServiceProvidersNoFormationAgent = TcspTypes(Set(
      NomineeShareholdersProvider,
      TrusteeProvider,
      CompanyDirectorEtc))

  }

  object NewValues {

    val NewProvidedServices = ProvidedServices(Set(EmailHandling))
    val NewCompanyServiceProviders = TcspTypes(Set(NomineeShareholdersProvider,
      CompanyFormationAgent))
    val NewServicesOfAnotherTCSP = ServicesOfAnotherTCSPNo

  }

  val completeJson = Json.obj(
    "tcspTypes" -> Json.obj(
      "serviceProviders" -> Seq("01", "02", "04", "05")
    ),
    "onlyOffTheShelfCompsSold" -> Json.obj(
      "onlyOffTheShelfCompsSold" -> true
    ),
    "complexCorpStructureCreation" -> Json.obj(
      "complexCorpStructureCreation" -> false
    ),
    "providedServices" -> Json.obj(
      "services" -> Seq("01", "08"),
      "details" -> "other service"
    ),
    "doesServicesOfAnotherTCSP" -> true,
    "servicesOfAnotherTCSP" -> Json.obj(
      "servicesOfAnotherTCSP" -> true,
      "mlrRefNumber" -> "12345678"
    ),
    "hasChanged" -> false,
    "hasAccepted" -> true
  )

  val completeModel = Tcsp(
    Some(DefaultValues.DefaultCompanyServiceProviders),
    Some(OnlyOffTheShelfCompsSoldYes),
    Some(ComplexCorpStructureCreationNo),
    Some(DefaultValues.DefaultProvidedServices),
    Some(true),
    Some(DefaultValues.DefaultServicesOfAnotherTCSP),
    hasAccepted = true
  )
}

class TcspSpec extends AmlsSpec with TcspValues {

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

    "have a task row function that" must {

      implicit val cache: Cache = mock[Cache]

      "returns a Not Started task row when model is empty" in {

        val notStartedTaskRow = TaskRow(
          "tcsp", controllers.tcsp.routes.WhatYouNeedController.get().url, false, NotStarted, TaskRow.notStartedTag
        )

        when(cache.getEntry[Tcsp]("tcsp")) thenReturn None

        Tcsp.taskRow mustBe notStartedTaskRow
      }

      "returns a Completed task row when model is complete" in {

        val complete = mock[Tcsp]
        val completedTaskRow = TaskRow(
          "tcsp", controllers.tcsp.routes.SummaryController.get().url, false, Completed, TaskRow.completedTag
        )

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Tcsp]("tcsp")) thenReturn Some(complete)

        Tcsp.taskRow mustBe completedTaskRow
      }

      "returns an Updated task row when model is complete" in {

        val complete = mock[Tcsp]
        val completedTaskRow = TaskRow(
          "tcsp", controllers.tcsp.routes.SummaryController.get().url, true, Updated, TaskRow.updatedTag
        )

        when(complete.isComplete) thenReturn true
        when(complete.hasChanged) thenReturn true
        when(cache.getEntry[Tcsp]("tcsp")) thenReturn Some(complete)

        Tcsp.taskRow mustBe completedTaskRow
      }

      "returns an Incomplete task row when model is incomplete" in {

        val incompleteTcsp = mock[Tcsp]
        val incompleteTaskRow = TaskRow(
          "tcsp", controllers.tcsp.routes.WhatYouNeedController.get().url, false, Started, TaskRow.incompleteTag
        )

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Tcsp]("tcsp")) thenReturn Some(incompleteTcsp)

        Tcsp.taskRow mustBe incompleteTaskRow
      }
    }

    "Serialise" in {
      Json.toJson(completeModel) must be(completeJson)
    }

    "Deserialise" when {
      "complete json is present" in {
        completeJson.as[Tcsp] must be(completeModel)
      }
      "doesServicesOfAnotherTCSP is absent" in {

        val completeJson = Json.obj(
          "tcspTypes" -> Json.obj(
            "serviceProviders" -> Seq("01", "02", "04", "05")
          ),
          "onlyOffTheShelfCompsSold" -> Json.obj(
            "onlyOffTheShelfCompsSold" -> true
          ),
          "complexCorpStructureCreation" -> Json.obj(
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

        completeJson.as[Tcsp] must be(completeModel)
      }
    }

    "Deserialise old format" when {
      "complete json is present" in {
        completeJson.as[Tcsp] must be(completeModel)
      }
      "doesServicesOfAnotherTCSP is absent" in {

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
          "doesServicesOfAnotherTCSP" -> true,
          "servicesOfAnotherTCSP" -> Json.obj(
            "servicesOfAnotherTCSP" -> true,
            "mlrRefNumber" -> "12345678"
          ),
          "hasChanged" -> false,
          "hasAccepted" -> true
        )

        completeJson.as[Tcsp] must be(completeModel)
      }
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
    "return true" when {
      "all fields are defined" in {
        completeModel.isComplete must be(true)
      }
      "providedServices is not defined" when {
        "tcspTypes does not contain RegisteredOfficeEtc" in {
          val completeModel = Tcsp(
            Some(DefaultValues.DefaultCompanyServiceProviders),
            Some(OnlyOffTheShelfCompsSoldYes),
            Some(ComplexCorpStructureCreationNo),
            None,
            Some(true),
            Some(DefaultValues.DefaultServicesOfAnotherTCSP),
            hasAccepted = true
          )
          completeModel.isComplete must be(true)
        }
      }

      "onlyOffTheShelfCompsSold is not defined" when {
        "tcspTypes does contain CompanyFormationAgent" in {
          val completeModel = Tcsp(
            tcspTypes = Some(DefaultValues.DefaultCompanyServiceProviders),
            onlyOffTheShelfCompsSold = None,
            complexCorpStructureCreation = Some(ComplexCorpStructureCreationNo),
            providedServices = None,
            doesServicesOfAnotherTCSP = Some(true),
            servicesOfAnotherTCSP = Some(DefaultValues.DefaultServicesOfAnotherTCSP),
            hasAccepted = true
          )
          completeModel.isComplete must be(false)
        }
      }
      "complexCorpStructureCreation is not defined" when {
        "tcspTypes does contain CompanyFormationAgent" in {
          val completeModel = Tcsp(
            tcspTypes = Some(DefaultValues.DefaultCompanyServiceProviders),
            onlyOffTheShelfCompsSold = Some(OnlyOffTheShelfCompsSoldYes),
            complexCorpStructureCreation = None,
            providedServices = None,
            doesServicesOfAnotherTCSP = Some(true),
            servicesOfAnotherTCSP = Some(DefaultValues.DefaultServicesOfAnotherTCSP),
            hasAccepted = true
          )
          completeModel.isComplete must be(false)
        }
      }

      "onlyOffTheShelfCompsSold is not defined" when {
        "tcspTypes does not contain CompanyFormationAgent" in {
          val completeModel = Tcsp(
            tcspTypes = Some(DefaultValues.DefaultCompanyServiceProvidersNoFormationAgent),
            onlyOffTheShelfCompsSold = None,
            complexCorpStructureCreation = None,
            providedServices = None,
            doesServicesOfAnotherTCSP = Some(true),
            servicesOfAnotherTCSP = Some(DefaultValues.DefaultServicesOfAnotherTCSP),
            hasAccepted = true
          )
          completeModel.isComplete must be(true)
        }
      }
      "complexCorpStructureCreation is not defined" when {
        "tcspTypes does not contain CompanyFormationAgent" in {
          val completeModel = Tcsp(
            tcspTypes = Some(DefaultValues.DefaultCompanyServiceProvidersNoFormationAgent),
            onlyOffTheShelfCompsSold = None,
            complexCorpStructureCreation = None,
            providedServices = None,
            doesServicesOfAnotherTCSP = Some(true),
            servicesOfAnotherTCSP = Some(DefaultValues.DefaultServicesOfAnotherTCSP),
            hasAccepted = true
          )
          completeModel.isComplete must be(true)
        }
      }

      "servicesOfAnotherTCSP is not defined" when {
        "doesServicesOfAnotherTCSP is false" in {
          val completeModel = Tcsp(
            Some(DefaultValues.DefaultCompanyServiceProviders),
            Some(OnlyOffTheShelfCompsSoldYes),
            Some(ComplexCorpStructureCreationNo),
            Some(DefaultValues.DefaultProvidedServices),
            Some(false),
            None,
            hasAccepted = true
          )
          completeModel.isComplete must be(true)
        }
      }

    }
    val initial: Option[Tcsp] = None

    "return false" when {
      "the model is empty" in {
        val incompleteModel = initial.copy(providedServices = None)
        incompleteModel.isComplete must be(false)
      }
      "providedServices is not defined" when {
        "tcspTypes does contain RegisteredOfficeEtc" in {
          val completeModel = Tcsp(
            Some(DefaultValues.DefaultCompanyServiceProviders),
            Some(OnlyOffTheShelfCompsSoldYes),
            Some(ComplexCorpStructureCreationNo),
            None,
            Some(true),
            Some(DefaultValues.DefaultServicesOfAnotherTCSP),
            hasAccepted = true
          )
          completeModel.isComplete must be(true)
        }
      }
      "servicesOfAnotherTCSP is not defined" when {
        "doesServicesOfAnotherTCSP is true" in {
          val completeModel = Tcsp(
            Some(DefaultValues.DefaultCompanyServiceProviders),
            Some(OnlyOffTheShelfCompsSoldYes),
            Some(ComplexCorpStructureCreationNo),
            Some(DefaultValues.DefaultProvidedServices),
            Some(true),
            None,
            hasAccepted = true
          )
          completeModel.isComplete must be(false)
        }
      }
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
