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

package models.responsiblepeople

import controllers.responsiblepeople.NinoUtil
import models.Country
import models.registrationprogress.{Completed, NotStarted, Started}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import org.joda.time.LocalDate
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeApplication
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants

class ResponsiblePeopleSpec extends PlaySpec with MockitoSugar with ResponsiblePeopleValues with OneAppPerSuite {

  "ResponsiblePeople" must {

    "serialise correctly" when {
      "residence and passport type is in current format" in {
        Json.toJson(completeModelNonUkResidentNonUkPassport) must be(completeJsonPresentNonUkResidentNonUkPassport)
      }
    }

    "deserialise current format json successfully" when {
      "json is complete" when {
        "uk resident = yes" in {
          completeJsonPresentUkResident.as[ResponsiblePeople] must be(completeModelUkResident)
        }

        "uk resident = yes in old data format" in {
          completeOldJsonPresentUkResident.as[ResponsiblePeople] must be(completeModelUkResidentForOldData)
        }
        "uk resident = no, uk passport = yes" in {
          completeJsonPresentNonUkResidentUkPassport.as[ResponsiblePeople] must be(completeModelNonUkResidentUkPassport)
        }
        "uk resident = no, uk passport = no, non-uk passport = yes" in {
          completeJsonPresentNonUkResidentNonUkPassport.as[ResponsiblePeople] must be(completeModelNonUkResidentNonUkPassport)
        }
        "uk resident = no, uk passport = no, non-uk passport = no" in {
          completeJsonPresentNonUkResidentNoPassport.as[ResponsiblePeople] must be(completeModelNonUkResidentNoPassport)
        }
      }
      "given partially complete json in current format" when {
        "response to Uk resident is no, and no further responses have been given" in {
          incompleteJsonCurrentUpToUkResident.as[ResponsiblePeople] must be(incompleteResponsiblePeopleUpToUkResident)
        }
        "response to Uk resident is no, and a uk passport number has been provided" in {
          incompleteJsonCurrentUpToUkPassportNumber.as[ResponsiblePeople] must be(incompleteResponsiblePeopleUpToUkPassportNumber)
        }
        "response to Uk resident is no, and a non-uk passport number has been provided" in {
          incompleteJsonCurrentUpToNonUkPassportNumber.as[ResponsiblePeople] must be(incompleteResponsiblePeopleUpToNonUkPassportNumber)
        }
        "response to Uk resident is no, non-uk passport is no and a date of birth has been given" in {
          incompleteJsonCurrentUpToNoNonUkPassportDateOfBirth.as[ResponsiblePeople] must be(incompleteResponsiblePeopleUpToNoNonUkPassportDateOfBirth)
        }
      }
      "given empty json" when {
        "no data is provided" in {
          Json.obj("hasChanged" -> false, "hasAccepted" -> false).as[ResponsiblePeople] must be(ResponsiblePeople())
        }
      }
    }

    "implicitly return an existing Model if one present" in {
      val responsiblePeople = ResponsiblePeople.default(Some(completeModelNonUkResidentNonUkPassport))
      responsiblePeople must be(completeModelNonUkResidentNonUkPassport)
    }

    "implicitly return an empty Model if not present" in {
      val responsiblePeople = ResponsiblePeople.default(None)
      responsiblePeople must be(ResponsiblePeople())
    }

    "the section" when {
      "has not been started" must {
        "direct the user to the add controller with what you need guidance requested" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(None)

          ResponsiblePeople.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true))
        }
      }

      "a partial address history has been given" must {
        "be marked as incomplete" in {
          val mockCacheMap = mock[CacheMap]

          when {
            mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any())
          } thenReturn Some(Seq(incompleteAddressHistoryPerson))

          ResponsiblePeople.section(mockCacheMap).status mustBe Started
        }
      }

      "is complete" must {
        "direct the user to the summary page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(Some(Seq(completeModelNonUkResidentNonUkPassport.copy(hasAccepted = true))))

          ResponsiblePeople.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.YourAnswersController.get())
        }
      }

      "is partially complete" must {
        "direct the user to the start of the the journey at the correct index for the incomplete item" in {
          val mockCacheMap = mock[CacheMap]

          val rp = Seq(completeModelNonUkResidentNonUkPassport, completeModelNonUkResidentNonUkPassport, incompleteResponsiblePeople) map (_.copy(hasAccepted = true))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(Some(rp))

          ResponsiblePeople.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.WhoMustRegisterController.get(3))

        }
      }

      "consists of just 1 empty Responsible Person" must {
        "return a result indicating NotStarted" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(Some(Seq(ResponsiblePeople())))

          ResponsiblePeople.section(mockCacheMap).status must be(models.registrationprogress.NotStarted)
        }
      }

      "consists of a partially complete model followed by a completely empty one" must {
        "return a result indicating partial completeness" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(Some(Seq(incompleteResponsiblePeople, ResponsiblePeople())))

          ResponsiblePeople.section(mockCacheMap).status must be(models.registrationprogress.Started)
        }
      }

      "has a completed model, an empty one and an incomplete one" when {
        "return the correct index" in {
          val mockCacheMap = mock[CacheMap]
          val rp = Seq(completeModelNonUkResidentNonUkPassport, ResponsiblePeople(), incompleteResponsiblePeople) map (_.copy(hasAccepted = true))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(Some(rp))

          ResponsiblePeople.section(mockCacheMap).call.url must be(controllers.responsiblepeople.routes.WhoMustRegisterController.get(3).url)
        }
      }
    }

    "The Default Model" when {

      val EmptyResponsiblePeople: Option[ResponsiblePeople] = None

      "Merged with personName" must {
        "return ResponsiblePeople with correct personName" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).personName(NewValues.personName)
          result must be(ResponsiblePeople(personName = Some(NewValues.personName), hasChanged = true))
        }
      }

      "Merged with PersonResidenceType" must {
        "return ResponsiblePeople with correct PersonResidenceType" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).personResidenceType(NewValues.personResidenceType)
          result must be(ResponsiblePeople(personResidenceType = Some(NewValues.personResidenceType), hasChanged = true))
        }
      }

      "Merged with ContactDetails" must {
        "return ResponsiblePeople with correct ContactDetails" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).contactDetails(NewValues.contactDetails)
          result must be(ResponsiblePeople(contactDetails = Some(NewValues.contactDetails), hasChanged = true))
        }
      }

      "Merged with AddressHistory" must {
        "return ResponsiblePeople with correct AddressHistory" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).addressHistory(NewValues.addressHistory)
          result must be(ResponsiblePeople(addressHistory = Some(NewValues.addressHistory), hasChanged = true))
        }
      }

      "Merged with Positions" must {
        "return ResponsiblePeople with correct Positions" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).positions(NewValues.positions)
          result must be(ResponsiblePeople(positions = Some(NewValues.positions), hasChanged = true))
        }
      }

      "Merged with SaRegistered" must {
        "return ResponsiblePeople with correct SaRegistered" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).saRegistered(NewValues.saRegistered)
          result must be(ResponsiblePeople(saRegistered = Some(NewValues.saRegistered), hasChanged = true))
        }
      }

      "Merged with VatRegistered" must {
        "return ResponsiblePeople with correct VatRegistered" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).vatRegistered(NewValues.vatRegistered)
          result must be(ResponsiblePeople(vatRegistered = Some(NewValues.vatRegistered), hasChanged = true))
        }
      }

      "Merged with experienceTraining" must {
        "return ResponsiblePeople with correct experienceTraining" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).experienceTraining(NewValues.experienceTraining)
          result must be(ResponsiblePeople(experienceTraining = Some(NewValues.experienceTraining), hasChanged = true))
        }
      }

      "Merged with Training" must {
        "return ResponsiblePeople with correct Training" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).training(NewValues.training)
          result must be(ResponsiblePeople(training = Some(NewValues.training), hasChanged = true))
        }
      }

      "Merged with FitAndProper" must {
        "return ResponsiblePeople with correct hasAlreadyPassedFitAndProper" in {
          val result = ResponsiblePeople.default(EmptyResponsiblePeople).hasAlreadyPassedFitAndProper(true)
          result must be(ResponsiblePeople(hasAlreadyPassedFitAndProper = Some(true), hasChanged = true))
        }
      }

    }

    "Successfully validate if the model is complete" when {

      "the model is fully complete" in {
        completeModelNonUkResidentNonUkPassport.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model partially complete with soleProprietorOfAnotherBusiness is empty" in {
        completeModelNonUkResidentNonUkPassport.copy(soleProprietorOfAnotherBusiness = None, hasAccepted = true).isComplete must be(true)
      }

      "the model partially complete with vat registration model is empty" in {
        completeModelNonUkResidentNonUkPassport.copy(vatRegistered = None).isComplete must be(false)
      }

      "the model partially complete soleProprietorOfAnotherBusiness is selected as No vat registration is not empty" in {
        completeModelNonUkResidentNonUkPassport.copy(soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)),
          vatRegistered = Some(VATRegisteredNo)).isComplete must be(false)
      }

      "the model is not complete" in {
        val initial = ResponsiblePeople(Some(DefaultValues.personName))
        initial.isComplete must be(false)
      }

    }

    "Amendment and Variation flow" when {
      "the section is complete with all the Responsible People being removed" must {
        "successfully redirect to what you need page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(Some(Seq(ResponsiblePeople(status = Some(StatusConstants.Deleted), hasChanged = true),
              ResponsiblePeople(status = Some(StatusConstants.Deleted), hasChanged = true))))
          val section = ResponsiblePeople.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(NotStarted)
          section.call must be(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true))
        }
      }

      "the section is complete with all the Responsible People being removed and has one incomplete model" must {
        "successfully redirect to what you need page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(Some(Seq(
              ResponsiblePeople(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
              ResponsiblePeople(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
              ResponsiblePeople(Some(DefaultValues.personName), hasAccepted = true))
            ))
          val section = ResponsiblePeople.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Started)
          section.call must be(controllers.responsiblepeople.routes.WhoMustRegisterController.get(3))
        }
      }

      "the section is complete with one of the Responsible People object being removed" must {
        "successfully redirect to check your answers page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
            .thenReturn(Some(Seq(
              ResponsiblePeople(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
              completeModelNonUkResidentNonUkPassport.copy(hasAccepted = true)
            )))
          val section = ResponsiblePeople.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.responsiblepeople.routes.YourAnswersController.get())
        }
      }
    }

  }

  it when {
    "personName value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.personName(DefaultValues.personName)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.personName(NewValues.personName)
          result must be(completeModelNonUkResidentNonUkPassport.copy(personName = Some(NewValues.personName), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "personResidenceType value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.personResidenceType(DefaultValues.personResidenceTypeNonUk)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.personResidenceType(NewValues.personResidenceType)
          result must be(completeModelNonUkResidentNonUkPassport.copy(personResidenceType = Some(NewValues.personResidenceType), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "contactDetails value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.contactDetails(DefaultValues.contactDetails)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.contactDetails(NewValues.contactDetails)
          result must be(completeModelNonUkResidentNonUkPassport.copy(contactDetails = Some(NewValues.contactDetails), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "addressHistory value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.addressHistory(DefaultValues.addressHistory)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.addressHistory(NewValues.addressHistory)
          result must be(completeModelNonUkResidentNonUkPassport.copy(addressHistory = Some(NewValues.addressHistory), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "positions value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.positions(DefaultValues.positions)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.positions(NewValues.positions)
          result must be(completeModelNonUkResidentNonUkPassport.copy(positions = Some(NewValues.positions), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "saRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.saRegistered(DefaultValues.saRegistered)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.saRegistered(NewValues.saRegistered)
          result must be(completeModelNonUkResidentNonUkPassport.copy(saRegistered = Some(NewValues.saRegistered), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "vatRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.vatRegistered(DefaultValues.vatRegistered)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.vatRegistered(NewValues.vatRegistered)
          result must be(completeModelNonUkResidentNonUkPassport.copy(vatRegistered = Some(NewValues.vatRegistered), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "experienceTraining value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.experienceTraining(DefaultValues.experienceTraining)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.experienceTraining(NewValues.experienceTraining)
          result must be(completeModelNonUkResidentNonUkPassport.copy(experienceTraining = Some(NewValues.experienceTraining), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "training value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.training(DefaultValues.training)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.training(NewValues.training)
          result must be(completeModelNonUkResidentNonUkPassport.copy(training = Some(NewValues.training), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "hasAlreadyPassedFitAndProper value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.hasAlreadyPassedFitAndProper(true)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.hasAlreadyPassedFitAndProper(false)
          result must be(completeModelNonUkResidentNonUkPassport.copy(hasAlreadyPassedFitAndProper = Some(false), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "ukPassport value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.ukPassport(UKPassportNo)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.ukPassport(UKPassportYes("87654321"))
          result must be(completeModelNonUkResidentNonUkPassport.copy(ukPassport = Some(UKPassportYes("87654321")), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "nonUKPassport value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.nonUKPassport(NonUKPassportYes("87654321"))
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.nonUKPassport(NoPassport)
          result must be(completeModelNonUkResidentNonUkPassport.copy(nonUKPassport = Some(NoPassport), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "dateOfBirth value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.dateOfBirth(DateOfBirth(new LocalDate(1990, 10, 2)))
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.dateOfBirth(DateOfBirth(new LocalDate(1990, 12, 12)))
          result must be(completeModelNonUkResidentNonUkPassport.copy(dateOfBirth = Some(DateOfBirth(new LocalDate(1990, 12, 12))), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "status value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = completeModelNonUkResidentNonUkPassport.status(StatusConstants.Unchanged)
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.status(StatusConstants.Deleted)
          result must be(completeModelNonUkResidentNonUkPassport.copy(status = Some(StatusConstants.Deleted), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
  }

  "anyChanged" must {
    val originalResponsiblePeople = Seq(completeModelNonUkResidentNonUkPassport)
    val responsiblePeopleChanged = Seq(completeModelNonUkResidentNonUkPassport.copy(hasChanged = true))

    "return false" when {
      "no ResponsiblePeople within the sequence have changed" in {
        val res = ResponsiblePeople.anyChanged(originalResponsiblePeople)
        res must be(false)
      }
    }
    "return true" when {
      "at least one ResponsiblePeople within the sequence has changed" in {
        val res = ResponsiblePeople.anyChanged(responsiblePeopleChanged)
        res must be(true)
      }
    }
  }
}

class ResponsiblePeopleWithoutHasAcceptedSpec extends PlaySpec with MockitoSugar with ResponsiblePeopleValues with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.has-accepted" -> false))

  "Successfully validate if the model is complete" when {

    "the model is fully complete" in {
      completeModelNonUkResidentNonUkPassport.isComplete must be(true)
    }

    "the model partially complete with soleProprietorOfAnotherBusiness is empty" in {
      completeModelNonUkResidentNonUkPassport.copy(soleProprietorOfAnotherBusiness = None, hasAccepted = true).isComplete must be(true)
    }

    "the model partially complete with vat registration model is empty" in {
      completeModelNonUkResidentNonUkPassport.copy(vatRegistered = None).isComplete must be(false)
    }

    "the model partially complete soleProprietorOfAnotherBusiness is selected as No vat registration is not empty" in {
      completeModelNonUkResidentNonUkPassport.copy(soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)),
        vatRegistered = Some(VATRegisteredNo)).isComplete must be(false)
    }

    "the model is not complete" in {
      val initial = ResponsiblePeople(Some(DefaultValues.personName))
      initial.isComplete must be(false)
    }

    "the model is has no data" in {
      val initial = ResponsiblePeople()
      initial.isComplete must be(true)
    }

  }

  "the section" when {
    "consists of a complete model followed by an empty one" must {
      "return a result indicating completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(Some(Seq(completeModelNonUkResidentNonUkPassport, ResponsiblePeople())))

        ResponsiblePeople.section(mockCacheMap).status must be(models.registrationprogress.Completed)
      }
    }
  }

}

trait ResponsiblePeopleValues extends NinoUtil {

  private val startDate = Some(new LocalDate())
  private val nino = nextNino

  object DefaultValues {

    val residenceNonUk = NonUKResidence
    val residenceUk = UKResidence("AA111111A")
    val residenceCountry = Country("United Kingdom", "GB")
    val residenceNationality = Country("United Kingdom", "GB")
    val currentPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "AA111AA")
    val currentAddress = ResponsiblePersonCurrentAddress(currentPersonAddress, Some(ZeroToFiveMonths))
    val additionalPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "AA11AA")
    val additionalAddress = ResponsiblePersonAddress(additionalPersonAddress, Some(OneToThreeYears))
    val soleProprietorOfAnotherBusiness = SoleProprietorOfAnotherBusiness(true)
    //scalastyle:off magic.number
    val personName = PersonName("first", Some("middle"), "last")
    val legalName = PreviousName(Some("oldFirst"), Some("oldMiddle"), Some("oldLast"))
    val knownBy = KnownBy(Some("name"))
    val personResidenceTypeNonUk = PersonResidenceType(residenceNonUk, Some(residenceCountry), Some(residenceNationality))
    val personResidenceTypeUk = PersonResidenceType(residenceUk, Some(residenceCountry), Some(residenceNationality))
    val saRegistered = SaRegisteredYes("0123456789")
    val contactDetails = ContactDetails("07702743555", "test@test.com")
    val addressHistory = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val vatRegistered = VATRegisteredNo
    val training = TrainingYes("test")
    val experienceTraining = ExperienceTrainingYes("Some training")
    val positions = Positions(Set(BeneficialOwner, InternalAccountant), startDate)
    val ukPassportYes = UKPassportYes("000000000")
    val ukPassportNo = UKPassportNo
    val nonUKPassportYes = NonUKPassportYes("87654321")
    val nonUKPassportNo = NoPassport
    val dateOfBirth = DateOfBirth(new LocalDate(1990, 10, 2))
  }

  object NewValues {

    private val residenceYear = 1990
    private val residenceMonth = 2
    private val residenceDay = 24
    private val residenceDate = new LocalDate(residenceYear, residenceMonth, residenceDay)
    private val residence = UKResidence(nino)
    private val residenceCountry = Country("United Kingdom", "GB")
    private val residenceNationality = Country("United Kingdom", "GB")
    private val newPersonAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
    private val newAdditionalPersonAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("France", "FR"))
    private val currentAddress = ResponsiblePersonCurrentAddress(newPersonAddress, Some(ZeroToFiveMonths))
    private val additionalAddress = ResponsiblePersonAddress(newAdditionalPersonAddress, Some(ZeroToFiveMonths))

    val personName = PersonName("firstnew", Some("middle"), "last")
    val legalName = PreviousName(Some("oldFirst"), Some("oldMiddle"), Some("oldLast"))
    val contactDetails = ContactDetails("07000000000", "new@test.com")
    val addressHistory = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val personResidenceType = PersonResidenceType(residence, Some(residenceCountry), Some(residenceNationality))
    val saRegistered = SaRegisteredNo
    val vatRegistered = VATRegisteredYes("12345678")
    val positions = Positions(Set(Director, SoleProprietor), startDate)
    val experienceTraining = ExperienceTrainingNo
    val training = TrainingNo
  }

  val completeModelUkResident = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    None,
    Some(DefaultValues.personResidenceTypeUk),
    None,
    None,
    None,
    Some(DefaultValues.contactDetails),
    Some(DefaultValues.addressHistory),
    Some(DefaultValues.positions),
    Some(DefaultValues.saRegistered),
    Some(DefaultValues.vatRegistered),
    Some(DefaultValues.experienceTraining),
    Some(DefaultValues.training),
    Some(true),
    false,
    false,
    Some(1),
    Some(StatusConstants.Unchanged),
    None,
    Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentForOldData = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    None,
    Some(DefaultValues.personResidenceTypeUk),
    None,
    None,
    None,
    Some(DefaultValues.contactDetails),
    Some(DefaultValues.addressHistory),
    Some(DefaultValues.positions),
    Some(DefaultValues.saRegistered),
    Some(DefaultValues.vatRegistered),
    Some(DefaultValues.experienceTraining),
    Some(DefaultValues.training),
    Some(true),
    false,
    false,
    Some(1),
    Some(StatusConstants.Unchanged),
    None,
    Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelNonUkResidentNonUkPassport = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    Some(DefaultValues.knownBy),
    Some(DefaultValues.personResidenceTypeNonUk),
    Some(DefaultValues.ukPassportNo),
    Some(DefaultValues.nonUKPassportYes),
    Some(DefaultValues.dateOfBirth),
    Some(DefaultValues.contactDetails),
    Some(DefaultValues.addressHistory),
    Some(DefaultValues.positions),
    Some(DefaultValues.saRegistered),
    Some(DefaultValues.vatRegistered),
    Some(DefaultValues.experienceTraining),
    Some(DefaultValues.training),
    Some(true),
    false,
    false,
    Some(1),
    Some(StatusConstants.Unchanged),
    None,
    Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelNonUkResidentNoPassport = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    None,
    Some(DefaultValues.personResidenceTypeNonUk),
    Some(DefaultValues.ukPassportNo),
    Some(DefaultValues.nonUKPassportNo),
    Some(DefaultValues.dateOfBirth),
    Some(DefaultValues.contactDetails),
    Some(DefaultValues.addressHistory),
    Some(DefaultValues.positions),
    Some(DefaultValues.saRegistered),
    Some(DefaultValues.vatRegistered),
    Some(DefaultValues.experienceTraining),
    Some(DefaultValues.training),
    Some(true),
    false,
    false,
    Some(1),
    Some(StatusConstants.Unchanged),
    None,
    Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelNonUkResidentUkPassport = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    None,
    Some(DefaultValues.personResidenceTypeNonUk),
    Some(DefaultValues.ukPassportYes),
    None,
    Some(DefaultValues.dateOfBirth),
    Some(DefaultValues.contactDetails),
    Some(DefaultValues.addressHistory),
    Some(DefaultValues.positions),
    Some(DefaultValues.saRegistered),
    Some(DefaultValues.vatRegistered),
    Some(DefaultValues.experienceTraining),
    Some(DefaultValues.training),
    Some(true),
    false,
    false,
    Some(1),
    Some(StatusConstants.Unchanged),
    None,
    Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val incompleteAddressHistoryPerson = completeModelUkResident.copy(
    addressHistory = Some(DefaultValues.addressHistory.copy(
      currentAddress = Some(DefaultValues.currentAddress.copy(timeAtAddress = Some(ZeroToFiveMonths))),
      additionalAddress = Some(DefaultValues.additionalAddress.copy(timeAtAddress = Some(SixToElevenMonths)))
    )))

  val incompleteResponsiblePeople = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    Some(DefaultValues.knownBy),
    Some(DefaultValues.personResidenceTypeNonUk),
    None,
    None,
    None,
    Some(DefaultValues.contactDetails),
    Some(DefaultValues.addressHistory)
  )

  val incompleteResponsiblePeopleUpToUkResident = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    None,
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk)
  )

  val incompleteResponsiblePeopleUpToUkPassportNumber = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    None,
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = Some(DefaultValues.ukPassportYes)
  )

  val incompleteResponsiblePeopleUpToNonUkPassportNumber = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    None,
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = None,
    nonUKPassport = Some(DefaultValues.nonUKPassportYes)
  )

  val incompleteResponsiblePeopleUpToNoNonUkPassportDateOfBirth = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.legalName),
    Some(new LocalDate(1990, 2, 24)),
    None,
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = None,
    nonUKPassport = Some(DefaultValues.nonUKPassportNo),
    dateOfBirth = Some(DefaultValues.dateOfBirth)
  )

  val incompleteJsonCurrent = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "false",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory" -> Json.obj(
      "currentAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      )
    )
  )

  val incompleteJsonCurrentUpToUkResident = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "false",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    )
  )

  val incompleteJsonCurrentUpToUkPassportNumber = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "false",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "ukPassport" -> Json.obj(
      "ukPassport" -> true,
      "ukPassportNumber" -> "000000000"
    )
  )

  val incompleteJsonCurrentUpToNonUkPassportNumber = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "false",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "nonUKPassport" -> Json.obj(
      "nonUKPassport" -> true,
      "nonUKPassportNumber" -> "87654321"
    )
  )

  val incompleteJsonCurrentUpToNoNonUkPassportDateOfBirth = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "false",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "nonUKPassport" -> Json.obj(
      "nonUKPassport" -> false
    ),
    "dateOfBirth" -> Json.obj(
      "dateOfBirth" -> "1990-10-02"
    )
  )

  val CompleteJsonPastNonUk = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> false,
      "dateOfBirth" -> "1990-10-02",
      "nonUKPassportNumber" -> "87654321",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory" -> Json.obj(
      "currentAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions" -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.toString("yyyy-MM-dd")
    ),
    "saRegistered" -> Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> "0123456789"
    ),
    "vatRegistered" -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining" -> Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> "Some training"
    ),
    "training" -> Json.obj(
      "training" -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper" -> true,
    "hasChanged" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged"
  )

  val incompleteJsonPastUk = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    )
  )

  val CompleteJsonPastUk = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "personResidenceType" -> Json.obj(
      "nino" -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory" -> Json.obj(
      "currentAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions" -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.toString("yyyy-MM-dd")
    ),
    "saRegistered" -> Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> "0123456789"
    ),
    "vatRegistered" -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining" -> Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> "Some training"
    ),
    "training" -> Json.obj(
      "training" -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper" -> true,
    "hasChanged" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged"
  )

  val completeJsonPresentNonUkResidentUkPassport = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24"
    ,
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "false",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "ukPassport" -> Json.obj(
      "ukPassport" -> true,
      "ukPassportNumber" -> "000000000"
    ),
    "dateOfBirth" -> Json.obj(
      "dateOfBirth" -> "1990-10-02"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory" -> Json.obj(
      "currentAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions" -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.toString("yyyy-MM-dd")
    ),
    "saRegistered" -> Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> "0123456789"
    ),
    "vatRegistered" -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining" -> Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> "Some training"
    ),
    "training" -> Json.obj(
      "training" -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper" -> true,
    "hasChanged" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged"
  )

  val completeJsonPresentNonUkResidentNonUkPassport = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24"
    ,"knownBy" -> Json.obj(
      "otherNames" -> "name"
    ),
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "false",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "ukPassport" -> Json.obj(
      "ukPassport" -> false
    ),
    "nonUKPassport" -> Json.obj(
      "nonUKPassport" -> true,
      "nonUKPassportNumber" -> "87654321"
    ),
    "dateOfBirth" -> Json.obj(
      "dateOfBirth" -> "1990-10-02"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory" -> Json.obj(
      "currentAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions" -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.toString("yyyy-MM-dd")
    ),
    "saRegistered" -> Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> "0123456789"
    ),
    "vatRegistered" -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining" -> Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> "Some training"
    ),
    "training" -> Json.obj(
      "training" -> true,
      "information" -> "test"
    ),
    "hasAlreadyPassedFitAndProper" -> true,
    "hasChanged" -> false,
    "hasAccepted" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged",
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    )
  )

  val completeJsonPresentNonUkResidentNoPassport = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24"
    ,
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "false",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "ukPassport" -> Json.obj(
      "ukPassport" -> false
    ),
    "nonUKPassport" -> Json.obj(
      "nonUKPassport" -> false
    ),
    "dateOfBirth" -> Json.obj(
      "dateOfBirth" -> "1990-10-02"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory" -> Json.obj(
      "currentAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions" -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.toString("yyyy-MM-dd")
    ),
    "saRegistered" -> Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> "0123456789"
    ),
    "vatRegistered" -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining" -> Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> "Some training"
    ),
    "training" -> Json.obj(
      "training" -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper" -> true,
    "hasChanged" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged"
  )


  val completeJsonPresentUkResident = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "KnownBy" -> Json.obj(
      "otherName" -> "name"
    ),
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "true",
      "nino" -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory" -> Json.obj(
      "currentAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions" -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.toString("yyyy-MM-dd")
    ),
    "saRegistered" -> Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> "0123456789"
    ),
    "vatRegistered" -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining" -> Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> "Some training"
    ),
    "training" -> Json.obj(
      "training" -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper" -> true,
    "hasChanged" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged"
  )

  val completeOldJsonPresentUkResident = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last",
      "previousName" -> Json.obj(
        "firstName" -> "oldFirst",
        "middleName" -> "oldMiddle",
        "lastName" -> "oldLast",
        "date" -> "1990-02-24"
      )
    ),
    "personResidenceType" -> Json.obj(
      "isUKResidence" -> "true",
      "nino" -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory" -> Json.obj(
      "currentAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1" -> "Line 1",
          "personAddressLine2" -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions" -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.toString("yyyy-MM-dd")
    ),
    "saRegistered" -> Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> "0123456789"
    ),
    "vatRegistered" -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining" -> Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> "Some training"
    ),
    "training" -> Json.obj(
      "training" -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper" -> true,
    "hasChanged" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged"
  )

}