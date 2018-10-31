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
import uk.gov.hmrc.domain.Nino
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

trait ResponsiblePeopleValues extends NinoUtil {

  private val startDate = Some(new LocalDate())
  private val nino = nextNino

  object DefaultValues {

    val residenceNonUk = NonUKResidence
    val residenceUk = UKResidence(Nino("AA111111A"))
    val residenceCountry = Country("United Kingdom", "GB")
    val residenceNationality = Country("United Kingdom", "GB")
    val currentPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "AA111AA")
    val currentAddress = ResponsiblePersonCurrentAddress(currentPersonAddress, Some(ZeroToFiveMonths))
    val additionalPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "AA11AA")
    val additionalAddress = ResponsiblePersonAddress(additionalPersonAddress, Some(OneToThreeYears))
    val soleProprietorOfAnotherBusiness = SoleProprietorOfAnotherBusiness(true)
    //scalastyle:off magic.number
    val personName = PersonName("first", Some("middle"), "last")
    val legalName = PreviousName(Some(true), Some("oldFirst"), Some("oldMiddle"), Some("oldLast"))
    val noPreviousName = PreviousName(Some(false), None, None, None)
    val knownBy = KnownBy(Some(true),Some("name"))
    val noKnownBy = KnownBy(Some(false),None)
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
    private val residence = UKResidence(Nino(nino))
    private val residenceCountry = Country("United Kingdom", "GB")
    private val residenceNationality = Country("United Kingdom", "GB")
    private val newPersonAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
    private val newAdditionalPersonAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("France", "FR"))
    private val currentAddress = ResponsiblePersonCurrentAddress(newPersonAddress, Some(ZeroToFiveMonths))
    private val additionalAddress = ResponsiblePersonAddress(newAdditionalPersonAddress, Some(ZeroToFiveMonths))

    val personName = PersonName("firstnew", Some("middle"), "last")
    val legalName = PreviousName(Some(true),Some("oldFirst"), Some("oldMiddle"), Some("oldLast"))
    val contactDetails = ContactDetails("07000000000", "new@test.com")
    val addressHistory = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val personResidenceType = PersonResidenceType(residence, Some(residenceCountry), Some(residenceNationality))
    val saRegistered = SaRegisteredNo
    val vatRegistered = VATRegisteredYes("12345678")
    val positions = Positions(Set(Director, SoleProprietor), startDate)
    val experienceTraining = ExperienceTrainingNo
    val training = TrainingNo
  }

  val completeModelUkResident = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )
  val incompleteModelUkResidentNoDOBPhase2 = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentPhase2 = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = Some(DefaultValues.noKnownBy),
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(false)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentForOldData = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentForOldDataPhase2 = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentForOldDataNoPrevious = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = None,
    legalNameChangeDate = None,
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentForOldDataNoPreviousPhase2 = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = None,
    legalNameChangeDate = None,
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelNonUkResidentNonUkPassport = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = Some(DefaultValues.knownBy),
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = Some(DefaultValues.ukPassportNo),
    nonUKPassport = Some(DefaultValues.nonUKPassportYes),
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelNonUkResidentNonUkPassportNoPreviousName = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = Some(DefaultValues.knownBy),
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = Some(DefaultValues.ukPassportNo),
    nonUKPassport = Some(DefaultValues.nonUKPassportYes),
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentNoPreviousNamePhase2 = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = Some(DefaultValues.knownBy),
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = Some(DefaultValues.ukPassportYes),
    nonUKPassport = None,
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )


  val completeModelNonUkResidentNoPassport = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = Some(DefaultValues.ukPassportNo),
    nonUKPassport = Some(DefaultValues.nonUKPassportNo),
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelNonUkResidentUkPassport = ResponsiblePerson(
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
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
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

  val incompleteResponsiblePeople = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = Some(DefaultValues.knownBy),
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory)
  )

  val incompleteResponsiblePeopleUpToUkResident = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk)
  )

  val incompleteResponsiblePeopleUpToUkPassportNumber = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = Some(DefaultValues.ukPassportYes)
  )

  val incompleteResponsiblePeopleUpToNonUkPassportNumber = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = None,
    nonUKPassport = Some(DefaultValues.nonUKPassportYes)
  )

  val incompleteResponsiblePeopleUpToNoNonUkPassportDateOfBirth = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(new LocalDate(1990, 2, 24)),
    knownBy = None,
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
      "hasPreviousName" -> true,
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
      "hasPreviousName" -> true,
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
      "hasPreviousName" -> true,
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
      "hasPreviousName" -> true,
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
      "hasPreviousName" -> true,
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
      "hasPreviousName" -> true,
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24"
    ,"knownBy" -> Json.obj(
      "hasOtherNames" -> true,
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
    "approvalFlags" -> Json.obj("hasAlreadyPassedFitAndProper" -> true),
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
      "hasPreviousName" -> true,
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
      "hasPreviousName" -> true,
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "KnownBy" -> Json.obj(
      "hasOtherNames" -> true,
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

  val completeJsonPresentUkResidentFitAndProperPhase2 = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "hasPreviousName" -> true,
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "KnownBy" -> Json.obj(
      "hasOtherNames" -> true,
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
    "approvalFlags" -> Json.obj("hasAlreadyPassedFitAndProper" -> true),
    "hasChanged" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged"
  )

  val completeJsonPresentUkResidentFitAndProperApprovalPhase2 = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
    ),
    "legalName" -> Json.obj(
      "hasPreviousName" -> true,
      "firstName" -> "oldFirst",
      "middleName" -> "oldMiddle",
      "lastName" -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "KnownBy" -> Json.obj(
      "hasOtherNames" -> true,
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
    "approvalFlags" -> Json.obj("hasAlreadyPassedFitAndProper" -> true),
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
        "hasPreviousName" -> true,
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

  val completeOldJsonPresentUkResidentNoPrevious = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "first",
      "middleName" -> "middle",
      "lastName" -> "last"
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
              hasAlreadyPaidApprovalCheck = Some(true)
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