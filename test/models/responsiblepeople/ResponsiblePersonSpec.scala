/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants

class ResponsiblePersonSpec extends PlaySpec with MockitoSugar with ResponsiblePeopleValues with OneAppPerSuite {


  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.phase-2-changes" -> false)
    .build()

  "ResponsiblePeople" must {

          "phase 1 is true" when {

            "fitAndProper is true and approval is none" in {
              val inputRp = ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPassedFitAndProper = Some(true),
                  hasAlreadyPaidApprovalCheck =  None),
                hasAccepted = true,
                hasChanged = true)

              val expectedRp = ResponsiblePerson(
                approvalFlags = ApprovalFlags(),
                hasAccepted = true,
                hasChanged = true)

              inputRp.resetBasedOnApprovalFlags() mustBe (expectedRp)

            }

            "fitAndProper is true and approval is true" in {
              val inputRp = ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPassedFitAndProper = Some(true),
                  hasAlreadyPaidApprovalCheck = Some(true)),
                hasAccepted = true,
                hasChanged = true)

              val expectedRp = ResponsiblePerson(
                approvalFlags = ApprovalFlags(),
                hasAccepted = true,
                hasChanged = true)

              inputRp.resetBasedOnApprovalFlags() mustBe (expectedRp)

            }
          }


    "serialise correctly" when {
      "residence and passport type is in current format" in {
        Json.toJson(completeModelNonUkResidentNonUkPassport) must be(completeJsonPresentNonUkResidentNonUkPassport)
      }
    }

    "deserialise current format json successfully" when {
      "json is complete" when {
        "uk resident = yes" in {
          completeJsonPresentUkResident.as[ResponsiblePerson] must be(completeModelUkResident)
        }

        "uk resident = yes in old data format" in {
          completeOldJsonPresentUkResident.as[ResponsiblePerson] must be(completeModelUkResidentForOldData)
        }
        "uk resident = yes in old data format with no previous name" in {
          completeOldJsonPresentUkResidentNoPrevious.as[ResponsiblePerson] must be(completeModelUkResidentForOldDataNoPrevious)
        }
        "uk resident = no, uk passport = yes" in {
          completeJsonPresentNonUkResidentUkPassport.as[ResponsiblePerson] must be(completeModelNonUkResidentUkPassport)
        }
        "uk resident = no, uk passport = no, non-uk passport = yes" in {
          completeJsonPresentNonUkResidentNonUkPassport.as[ResponsiblePerson] must be(completeModelNonUkResidentNonUkPassport)
        }
        "uk resident = no, uk passport = no, non-uk passport = no" in {
          completeJsonPresentNonUkResidentNoPassport.as[ResponsiblePerson] must be(completeModelNonUkResidentNoPassport)
        }
      }
      "given partially complete json in current format" when {
        "response to Uk resident is no, and no further responses have been given" in {
          incompleteJsonCurrentUpToUkResident.as[ResponsiblePerson] must be(incompleteResponsiblePeopleUpToUkResident)
        }
        "response to Uk resident is no, and a uk passport number has been provided" in {
          incompleteJsonCurrentUpToUkPassportNumber.as[ResponsiblePerson] must be(incompleteResponsiblePeopleUpToUkPassportNumber)
        }
        "response to Uk resident is no, and a non-uk passport number has been provided" in {
          incompleteJsonCurrentUpToNonUkPassportNumber.as[ResponsiblePerson] must be(incompleteResponsiblePeopleUpToNonUkPassportNumber)
        }
        "response to Uk resident is no, non-uk passport is no and a date of birth has been given" in {
          incompleteJsonCurrentUpToNoNonUkPassportDateOfBirth.as[ResponsiblePerson] must be(incompleteResponsiblePeopleUpToNoNonUkPassportDateOfBirth)
        }
      }
      "given empty json" when {
        "no data is provided" in {
          Json.obj("hasChanged" -> false, "hasAccepted" -> false).as[ResponsiblePerson] must be(ResponsiblePerson())
        }
      }
    }

    "implicitly return an existing Model if one present" in {
      val responsiblePeople = ResponsiblePerson.default(Some(completeModelNonUkResidentNonUkPassport))
      responsiblePeople must be(completeModelNonUkResidentNonUkPassport)
    }

    "implicitly return an empty Model if not present" in {
      val responsiblePeople = ResponsiblePerson.default(None)
      responsiblePeople must be(ResponsiblePerson())
    }

    "the section" when {
      "has not been started" must {
        "direct the user to the add controller with what you need guidance requested" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(None)

          ResponsiblePerson.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true))
        }
      }

      "a partial address history has been given" must {
        "be marked as incomplete" in {
          val mockCacheMap = mock[CacheMap]

          when {
            mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())
          } thenReturn Some(Seq(incompleteAddressHistoryPerson))

          ResponsiblePerson.section(mockCacheMap).status mustBe Started
        }
      }

      "is complete" must {
        "direct the user to the summary page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(completeModelNonUkResidentNonUkPassport.copy(hasAccepted = true))))

          ResponsiblePerson.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get())
        }
      }

      "is partially complete" must {
        "direct the user to the 'Your responsible people' page to show the incomplete items" in {
          val mockCacheMap = mock[CacheMap]

          val rp = Seq(completeModelNonUkResidentNonUkPassport,
            completeModelNonUkResidentNonUkPassport,
            incompleteResponsiblePeople) map (_.copy(hasAccepted = true))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(rp))

          ResponsiblePerson.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get())
        }
      }

      "consists of just 1 empty Responsible Person" must {
        "return a result indicating NotStarted" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(ResponsiblePerson())))

          ResponsiblePerson.section(mockCacheMap).status must be(models.registrationprogress.NotStarted)
        }
      }

      "consists of a partially complete model followed by a completely empty one" must {
        "return a result indicating partial completeness" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(incompleteResponsiblePeople, ResponsiblePerson())))

          ResponsiblePerson.section(mockCacheMap).status must be(models.registrationprogress.Started)
        }
      }

      "has a completed model, an empty one and an incomplete one" when {
        "return the correct index" in {
          val mockCacheMap = mock[CacheMap]
          val rp = Seq(
            completeModelNonUkResidentNonUkPassport,
            ResponsiblePerson(),
            incompleteResponsiblePeople
          ) map (_.copy(hasAccepted = true))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(rp))

          ResponsiblePerson.section(mockCacheMap).call.url mustBe controllers.responsiblepeople.routes.YourResponsiblePeopleController.get().url
        }
      }
    }

    "The Default Model" when {

      val EmptyResponsiblePeople: Option[ResponsiblePerson] = None

      "Merged with personName" must {
        "return ResponsiblePeople with correct personName" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).personName(NewValues.personName)
          result must be(ResponsiblePerson(personName = Some(NewValues.personName), hasChanged = true))
        }
      }

      "Merged with PersonResidenceType" must {
        "return ResponsiblePeople with correct PersonResidenceType" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).personResidenceType(NewValues.personResidenceType)
          result must be(ResponsiblePerson(personResidenceType = Some(NewValues.personResidenceType), hasChanged = true))
        }
      }

      "Merged with ContactDetails" must {
        "return ResponsiblePeople with correct ContactDetails" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).contactDetails(NewValues.contactDetails)
          result must be(ResponsiblePerson(contactDetails = Some(NewValues.contactDetails), hasChanged = true))
        }
      }

      "Merged with AddressHistory" must {
        "return ResponsiblePeople with correct AddressHistory" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).addressHistory(NewValues.addressHistory)
          result must be(ResponsiblePerson(addressHistory = Some(NewValues.addressHistory), hasChanged = true))
        }
      }

      "Merged with Positions" must {
        "return ResponsiblePeople with correct Positions" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).positions(NewValues.positions)
          result must be(ResponsiblePerson(positions = Some(NewValues.positions), hasChanged = true))
        }
      }

      "Merged with SaRegistered" must {
        "return ResponsiblePeople with correct SaRegistered" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).saRegistered(NewValues.saRegistered)
          result must be(ResponsiblePerson(saRegistered = Some(NewValues.saRegistered), hasChanged = true))
        }
      }

      "Merged with VatRegistered" must {
        "return ResponsiblePeople with correct VatRegistered" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).vatRegistered(NewValues.vatRegistered)
          result must be(ResponsiblePerson(vatRegistered = Some(NewValues.vatRegistered), hasChanged = true))
        }
      }

      "Merged with experienceTraining" must {
        "return ResponsiblePeople with correct experienceTraining" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).experienceTraining(NewValues.experienceTraining)
          result must be(ResponsiblePerson(experienceTraining = Some(NewValues.experienceTraining), hasChanged = true))
        }
      }

      "Merged with Training" must {
        "return ResponsiblePeople with correct Training" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).training(NewValues.training)
          result must be(ResponsiblePerson(training = Some(NewValues.training), hasChanged = true))
        }
      }

      "Merged with FitAndProper" must {
        "return ResponsiblePeople with correct hasAlreadyPassedFitAndProper" in {
          val result = ResponsiblePerson.default(EmptyResponsiblePeople).approvalFlags(ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)))
          result must be(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)), hasChanged = true))
        }
      }

    }

    "Successfully validate if the model is complete" when {

      "the model is fully complete" in {
        completeModelNonUkResidentNonUkPassport.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model is fully complete with no previous name added" in {
        completeModelNonUkResidentNonUkPassportNoPreviousName.copy(hasAccepted = true).isComplete must be(true)
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
        val initial = ResponsiblePerson(Some(DefaultValues.personName))
        initial.isComplete must be(false)
      }

    }

    "Amendment and Variation flow" when {
      "the section is complete with all the Responsible People being removed" must {
        "successfully redirect to what you need page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(
              ResponsiblePerson(status = Some(StatusConstants.Deleted), hasChanged = true),
              ResponsiblePerson(status = Some(StatusConstants.Deleted), hasChanged = true))))

          val section = ResponsiblePerson.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(NotStarted)
          section.call must be(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true))
        }
      }

      "the section is complete with all the Responsible People being removed and has one incomplete model" must {
        "successfully redirect to the 'your responsible people' page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(
              completeModelUkResident.copy(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
              completeModelUkResident.copy(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
              ResponsiblePerson(Some(DefaultValues.personName), hasAccepted = true))
            ))

          val section = ResponsiblePerson.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Started)
          section.call must be(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get())
        }
      }

      "the section is complete with one of the Responsible People object being removed" must {
        "successfully redirect to 'your responsible people' page" in {
          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(
              completeModelUkResident.copy(status = Some(StatusConstants.Deleted), hasChanged = true, hasAccepted = true),
              completeModelNonUkResidentNonUkPassport.copy(hasAccepted = true)
            )))

          val section = ResponsiblePerson.section(mockCacheMap)

          section.hasChanged must be(true)
          section.status must be(Completed)
          section.call must be(controllers.responsiblepeople.routes.YourResponsiblePeopleController.get())
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
          val result = completeModelNonUkResidentNonUkPassport.approvalFlags(ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)))
          result must be(completeModelNonUkResidentNonUkPassport)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = completeModelNonUkResidentNonUkPassport.approvalFlags(ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)))
          result must be(completeModelNonUkResidentNonUkPassport.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)), hasChanged = true))
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
        val res = ResponsiblePerson.anyChanged(originalResponsiblePeople)
        res must be(false)
      }
    }
    "return true" when {
      "at least one ResponsiblePeople within the sequence has changed" in {
        val res = ResponsiblePerson.anyChanged(responsiblePeopleChanged)
        res must be(true)
      }
    }
  }
}

class ResponsiblePersonSpecWithPhase2Changes extends PlaySpec with MockitoSugar with ResponsiblePeopleValues with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.phase-2-changes" -> true)
    .build()

  "ResponsiblePeople" must {

    "calling updateFitAndProperAndApproval" must {

      val inputRp = ResponsiblePerson()

      "set Approval flag to None" when {

        "only if both choice and msbOrTcsp are false so the answer to the approval question is needed" in {

          val outputRp = inputRp.updateFitAndProperAndApproval(fitAndPropperChoice = false, msbOrTcsp = false)

          val expectedRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = None
            ),
            hasChanged = true
          )

          outputRp mustEqual (expectedRp)
        }
      }

      "set Approval to match the incoming fitAndProper flag" when {

        "choice is true, and msbOrTcsp is false so the answer to the approval question in not required" in {

          val outputRp = inputRp.updateFitAndProperAndApproval(fitAndPropperChoice = true, msbOrTcsp = false)

          val expectedRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(true),
              hasAlreadyPaidApprovalCheck = Some(true)
            ),
            hasChanged = true
          )

          outputRp mustEqual (expectedRp)
        }

        "choice is false, and msbOrTcsp is true so the answer to the approval question is not needed" in {

          val outputRp = inputRp.updateFitAndProperAndApproval(fitAndPropperChoice = false, msbOrTcsp = true)

          val expectedRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = Some(false)
            ),
            hasChanged = true
          )

          outputRp mustEqual (expectedRp)
        }
      }
    }

    "reset when resetBasedOnApprovalFlags is called" when {

      "phase 2 feature toggle is true" when {

        "fitAndProper is true and approval is true" in {
          val inputRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(true),
              hasAlreadyPaidApprovalCheck = Some(true)),
            hasAccepted = true,
            hasChanged = true)

          inputRp.resetBasedOnApprovalFlags() mustBe(inputRp)

        }

        "fitAndProper is false and approval is true" in {
          val inputRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = Some(true)),
            hasAccepted = true,
            hasChanged = true)

          val expectedRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = None),
            hasAccepted = false,
            hasChanged = true)

          inputRp.resetBasedOnApprovalFlags() mustBe(expectedRp)
        }
      }
    }

    "Successfully validate if the model is complete when phase 2 feature toggle is true" when {

      "json is complete" when {

        "both Fit and proper and approval are both set only" in {
          completeJsonPresentUkResidentFitAndProperPhase2.as[ResponsiblePerson] must be(completeModelUkResident)
        }

        "will fail if at least one of the approval flags is not defined" in {
          val model = completeModelUkResidentPhase2.copy(approvalFlags = ApprovalFlags(hasAlreadyPaidApprovalCheck = None))

          model.isComplete must be(false)
        }
      }

      "json is complete" when {
        "Fit and proper and approval" in {
          completeJsonPresentUkResidentFitAndProperApprovalPhase2.as[ResponsiblePerson] must be(completeModelUkResident)
        }
      }

      "the model is fully complete" in {
        completeModelUkResidentPhase2.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model is fully complete with no previous name added" in {
        completeModelUkResidentNoPreviousNamePhase2.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model partially complete with soleProprietorOfAnotherBusiness is empty" in {
        completeModelUkResidentPhase2.copy(soleProprietorOfAnotherBusiness = None, hasAccepted = true).isComplete must be(true)
      }

      "the model partially complete with vat registration model is empty" in {
        completeModelUkResidentPhase2.copy(vatRegistered = None).isComplete must be(false)
      }

      "the model partially complete soleProprietorOfAnotherBusiness is selected as No vat registration is not empty" in {
        completeModelUkResidentPhase2.copy(soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)),
          vatRegistered = Some(VATRegisteredNo)).isComplete must be(false)
      }

      "the model is incomplete" in {
        incompleteModelUkResidentNoDOBPhase2.copy(hasAccepted = true).isComplete must be(false)
      }

      "the model is not complete" in {
        val initial = ResponsiblePerson(Some(DefaultValues.personName))
        initial.isComplete must be(false)
      }
    }
  }
}