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

package models.responsiblepeople

import models.registrationprogress.NotStarted
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers
import services.cache.Cache
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

class ResponsiblePersonSpec extends PlaySpec with MockitoSugar with ResponsiblePeopleValues with GuiceOneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
    )
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

          outputRp mustEqual expectedRp
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

          outputRp mustEqual expectedRp
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

          outputRp mustEqual expectedRp
        }
      }
    }

    "reset when resetBasedOnApprovalFlags is called" when {
      "fitAndProper is true and approval is true" in {
        val inputRp = ResponsiblePerson(
          approvalFlags =
            ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
          hasAccepted = true,
          hasChanged = true
        )

        inputRp.resetBasedOnApprovalFlags() mustBe inputRp
      }

      "fitAndProper is false and approval is true" in {
        val inputRp = ResponsiblePerson(
          approvalFlags =
            ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = Some(true)),
          hasAccepted = true,
          hasChanged = true
        )

        val expectedRp = ResponsiblePerson(
          approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = None),
          hasAccepted = false,
          hasChanged = true
        )

        inputRp.resetBasedOnApprovalFlags() mustBe expectedRp
      }
    }

    "set hasPreviousName correctly" when {

      "given true" in {
        val updated = ResponsiblePerson(legalName = Some(PreviousName(None, None, None, None))).hasPreviousName(true)

        updated.legalName.flatMap(_.hasPreviousName).value mustBe true
      }

      "given false" in {
        val updated = ResponsiblePerson(
          legalName = Some(PreviousName(Some(true), Some("first"), Some("middle"), Some("last")))
        ).hasPreviousName(false)

        updated.legalName.flatMap(_.hasPreviousName).value mustBe false
      }
    }

    "Successfully validate if the model is complete" when {
      "json is complete" when {
        "both Fit and proper and approval are both set only" in {
          completeJsonPresentUkResidentFitAndProper.as[ResponsiblePerson] must be(completeModelUkResidentFPtrue)
        }

        "will fail if at least one of the approval flags is not defined" in {
          val model = completeModelUkResident.copy(approvalFlags = ApprovalFlags(hasAlreadyPaidApprovalCheck = None))

          model.isComplete must be(false)
        }
      }

      "json is complete" when {
        "Fit and proper and approval" in {
          completeJsonPresentUkResidentFitAndProperApproval.as[ResponsiblePerson] must be(completeModelUkResidentFPtrue)
        }
      }

      "the model is fully complete" in {
        completeModelUkResident.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model is fully complete with no previous name added" in {
        completeModelUkResidentNoPreviousName.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model partially complete with soleProprietorOfAnotherBusiness is empty" in {
        completeModelUkResident.copy(soleProprietorOfAnotherBusiness = None, hasAccepted = true).isComplete must be(
          true
        )
      }

      "the model partially complete with vat registration model is empty" in {
        completeModelUkResident.copy(vatRegistered = None).isComplete must be(false)
      }

      "the model partially complete soleProprietorOfAnotherBusiness is selected as No vat registration is not empty" in {
        completeModelUkResident
          .copy(
            soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)),
            vatRegistered = Some(VATRegisteredNo)
          )
          .isComplete must be(false)
      }

      "the model is incomplete" in {
        incompleteModelUkResidentNoDOB.copy(hasAccepted = true).isComplete must be(false)
      }

      "the model is not complete" in {
        val initial = ResponsiblePerson(Some(DefaultValues.personName))
        initial.isComplete must be(false)
      }
    }

    "have taskRow function which" when {

      val messages = Helpers.stubMessages()

      "called" must {
        "return NotStarted task row if model is empty" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(ResponsiblePerson())))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.NotStarted)
        }

        "return Started task row if model is not empty" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(incompleteResponsiblePeople)))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Started)
        }

        "return Completed task row if model is not empty and has complete rp" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(completeResponsiblePerson)))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Completed)
        }

        "return Completed task row if model is not empty and has complete rp that has changed" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(completeResponsiblePerson.copy(hasChanged = true))))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Updated)
        }

        "return Started task row if model is not empty and has complete and incomplete rps" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(completeResponsiblePerson, incompleteResponsiblePeople)))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Started)
        }
      }
    }
  }

  "setter methods" must {

    "personName setter sets hasAccepted correctly when value matches" in {
      val name = DefaultValues.personName
      val rp   = ResponsiblePerson(personName = Some(name), hasAccepted = true)
      rp.personName(name).hasAccepted mustBe true
    }

    "legalName setter sets hasAccepted correctly when value matches" in {
      val name = PreviousName(Some(true), Some("first"), None, Some("last"))
      val rp   = ResponsiblePerson(legalName = Some(name), hasAccepted = true)
      rp.legalName(name).hasAccepted mustBe true
    }

    "hasPreviousName setter hasAccepted is always false due to contains type mismatch" in {
      val rp = ResponsiblePerson(legalName = Some(PreviousName(Some(true), None, None, None)), hasAccepted = true)
      rp.hasPreviousName(true).hasAccepted mustBe false
    }

    "legalNameChangeDate setter sets hasAccepted correctly when value matches" in {
      val date = LocalDate.of(2020, 1, 1)
      val rp   = ResponsiblePerson(legalNameChangeDate = Some(date), hasAccepted = true)
      rp.legalNameChangeDate(date).hasAccepted mustBe true
    }

    "knownBy setter hasAccepted is always false due to double-wrapping bug" in {
      val kb = KnownBy(Some(true), Some("name"))
      val rp = ResponsiblePerson(knownBy = Some(kb), hasAccepted = true)
      rp.knownBy(kb).hasAccepted mustBe false
    }

    "personResidenceType setter sets hasAccepted correctly when value matches" in {
      val prt = PersonResidenceType(UKResidence(Nino("AA000000A")), None, None)
      val rp  = ResponsiblePerson(personResidenceType = Some(prt), hasAccepted = true)
      rp.personResidenceType(prt).hasAccepted mustBe true
    }

    "contactDetails setter sets hasAccepted correctly when value matches" in {
      val cd = ContactDetails("07777777777", "test@test.com")
      val rp = ResponsiblePerson(contactDetails = Some(cd), hasAccepted = true)
      rp.contactDetails(cd).hasAccepted mustBe true
    }

    "saRegistered setter sets hasAccepted correctly when value matches" in {
      val sa = SaRegisteredYes("ref")
      val rp = ResponsiblePerson(saRegistered = Some(sa), hasAccepted = true)
      rp.saRegistered(sa).hasAccepted mustBe true
    }

    "addressHistory setter sets hasAccepted correctly when value matches" in {
      val ah = ResponsiblePersonAddressHistory(
        currentAddress = Some(
          ResponsiblePersonCurrentAddress(PersonAddressUK("l1", None, None, None, "AA1 1AA"), Some(ThreeYearsPlus))
        )
      )
      val rp = ResponsiblePerson(addressHistory = Some(ah), hasAccepted = true)
      rp.addressHistory(ah).hasAccepted mustBe true
    }

    "positions setter sets hasAccepted correctly when value matches" in {
      val pos = Positions(Set(BeneficialOwner), Some(PositionStartDate(LocalDate.of(2020, 1, 1))))
      val rp  = ResponsiblePerson(positions = Some(pos), hasAccepted = true)
      rp.positions(pos).hasAccepted mustBe true
    }

    "soleProprietorOfAnotherBusiness setter sets hasAccepted and hasChanged correctly" in {
      val sp = SoleProprietorOfAnotherBusiness(true)
      val rp = ResponsiblePerson(soleProprietorOfAnotherBusiness = Some(sp), hasAccepted = true)
      rp.soleProprietorOfAnotherBusiness(sp).hasAccepted mustBe true
      rp.soleProprietorOfAnotherBusiness(sp).hasChanged mustBe false
    }

    "vatRegistered setter sets hasAccepted correctly when value matches" in {
      val vat = VATRegisteredYes("123456789")
      val rp  = ResponsiblePerson(vatRegistered = Some(vat), hasAccepted = true)
      rp.vatRegistered(vat).hasAccepted mustBe true
    }

    "experienceTraining setter sets hasAccepted correctly when value matches" in {
      val et = ExperienceTrainingYes("some training")
      val rp = ResponsiblePerson(experienceTraining = Some(et), hasAccepted = true)
      rp.experienceTraining(et).hasAccepted mustBe true
    }

    "training setter sets hasAccepted correctly when value matches" in {
      val t  = TrainingYes("some training")
      val rp = ResponsiblePerson(training = Some(t), hasAccepted = true)
      rp.training(t).hasAccepted mustBe true
    }

    "ukPassport setter sets hasAccepted correctly when value matches" in {
      val passport = UKPassportYes("123456789")
      val rp       = ResponsiblePerson(ukPassport = Some(passport), hasAccepted = true)
      rp.ukPassport(passport).hasAccepted mustBe true
    }

    "nonUKPassport setter sets hasAccepted correctly when value matches" in {
      val passport = NonUKPassportYes("123456789")
      val rp       = ResponsiblePerson(nonUKPassport = Some(passport), hasAccepted = true)
      rp.nonUKPassport(passport).hasAccepted mustBe true
    }

    "dateOfBirth setter sets hasAccepted correctly when value matches" in {
      val dob = DateOfBirth(LocalDate.of(1990, 1, 1))
      val rp  = ResponsiblePerson(dateOfBirth = Some(dob), hasAccepted = true)
      rp.dateOfBirth(dob).hasAccepted mustBe true
    }

    "status setter sets hasAccepted and hasChanged correctly" in {
      val rp = ResponsiblePerson(status = Some("active"), hasAccepted = true)
      rp.status("active").hasAccepted mustBe true
      rp.status("active").hasChanged mustBe false
    }
  }

  "checkVatField" must {
    "return false when soleProprietor is false and vatRegistered is defined" in {
      val rp = ResponsiblePerson(vatRegistered = Some(VATRegisteredYes("123456789")))
      rp.checkVatField(Some(SoleProprietorOfAnotherBusiness(false))) mustBe false
    }
  }

  "validateAddressHistory" must {
    "return false when current address is short stay and no additional address" in {
      val ah = ResponsiblePersonAddressHistory(
        currentAddress = Some(
          ResponsiblePersonCurrentAddress(PersonAddressUK("l1", None, None, None, "AA1 1AA"), Some(ZeroToFiveMonths))
        )
      )
      ResponsiblePerson(addressHistory = Some(ah)).isComplete mustBe false
    }

    "return false when current and additional address are both short stay" in {
      val ah = ResponsiblePersonAddressHistory(
        currentAddress = Some(
          ResponsiblePersonCurrentAddress(PersonAddressUK("l1", None, None, None, "AA1 1AA"), Some(SixToElevenMonths))
        ),
        additionalAddress = Some(
          ResponsiblePersonAddress(PersonAddressUK("l2", None, None, None, "AA1 1AA"), Some(ZeroToFiveMonths))
        )
      )
      ResponsiblePerson(addressHistory = Some(ah)).isComplete mustBe false
    }
  }

  "isNominatedOfficer" must {
    "return false when positions is None" in {
      ResponsiblePerson().isNominatedOfficer mustBe false
    }
  }

  "hasNominatedOfficer" must {
    "return false when rpSeqOption is None" in {
      ResponsiblePerson.hasNominatedOfficer(None) mustBe false
    }

    "return false when no rp has nominated officer position" in {
      val rp = ResponsiblePerson(
        positions = Some(Positions(Set(BeneficialOwner), Some(PositionStartDate(LocalDate.of(2020, 1, 1)))))
      )
      ResponsiblePerson.hasNominatedOfficer(Some(Seq(rp))) mustBe false
    }
  }

  "getResponsiblePersonFromData" must {
    "return None when index is out of bounds" in {
      val rps = Seq(ResponsiblePerson())
      ResponsiblePerson.getResponsiblePersonFromData(Some(rps), 99) mustBe None
    }

    "return None when data is None" in {
      ResponsiblePerson.getResponsiblePersonFromData(None, 1) mustBe None
    }
  }

  "JSON reads" must {
    "strip nonUKPassport when ukPassport is present" in {
      val json   = completeJsonPresentUkResidentFitAndProper
      val result = json.as[ResponsiblePerson]
      result.nonUKPassport mustBe None
    }

    "keep rp as-is when both uk and non-uk passport and dob are absent" in {
      val rp     = completeModelUkResident.copy(ukPassport = None, nonUKPassport = None, dateOfBirth = None)
      val json   = Json.toJson(rp)
      val result = json.as[ResponsiblePerson]
      result.ukPassport mustBe None
      result.nonUKPassport mustBe None
    }

    "use existing legalName/knownBy/legalNameChangeDate when already present in JSON" in {
      val rp     = completeModelUkResident
      val json   = Json.toJson(rp)
      val result = json.as[ResponsiblePerson]
      result.legalName mustBe rp.legalName
      result.knownBy mustBe rp.knownBy
      result.legalNameChangeDate mustBe rp.legalNameChangeDate
    }
  }

  "hasUkPassportNumber / hasNonUkPassportNumber" must {
    "return true for UKPassportYes" in {
      val rp     = ResponsiblePerson(ukPassport = Some(UKPassportYes("123456789")))
      val result = Json.toJson(rp).as[ResponsiblePerson]
      result.nonUKPassport mustBe None
    }

    "return true for NonUKPassportYes" in {
      val rp     = ResponsiblePerson(nonUKPassport = Some(NonUKPassportYes("123456789")), ukPassport = Some(UKPassportNo))
      val result = Json.toJson(rp).as[ResponsiblePerson]
      result.nonUKPassport mustBe rp.nonUKPassport
    }
  }

  "filterEmpty and filterEmptyNoChanges" must {
    "filter out fully empty responsible people" in {
      val people = Seq(ResponsiblePerson(), completeResponsiblePerson)
      people.filterEmpty must not contain ResponsiblePerson()
    }

    "keep non-empty responsible people in filterEmpty" in {
      val people = Seq(completeResponsiblePerson)
      people.filterEmpty mustBe people
    }

    "filter out empty no-changes responsible people" in {
      val people = Seq(ResponsiblePerson(), completeResponsiblePerson)
      people.filterEmptyNoChanges must not contain ResponsiblePerson()
    }
  }

  "taskRow" must {
    "return NotStarted when cache returns None" in {
      val mockCacheMap = mock[Cache]
      when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
        .thenReturn(None)
      ResponsiblePerson.taskRow(mockCacheMap, Helpers.stubMessages()).status mustBe NotStarted
    }
  }
}
