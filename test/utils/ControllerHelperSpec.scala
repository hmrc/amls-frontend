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

package utils

import models.businessactivities._
import models.eab.Eab
import models.responsiblepeople._
import models.supervision._
import org.mockito.Mockito.when
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDate

class ControllerHelperSpec extends AmlsSpec with ResponsiblePeopleValues with DependencyMocks {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
    )
    .build()

  def createCompleteResponsiblePersonSeq: Option[Seq[ResponsiblePerson]] = Some(Seq(completeResponsiblePerson))

  def createRPsWithMissingDoB: Option[Seq[ResponsiblePerson]] = {
    val inCompleteResponsiblePerson: ResponsiblePerson = completeResponsiblePerson.copy(dateOfBirth = None)

    Some(
      Seq(
        completeResponsiblePerson,
        inCompleteResponsiblePerson,
        completeResponsiblePerson
      )
    )
  }

  val completeRedressScheme: JsObject = Json.obj(
    "redressScheme" -> "propertyOmbudsman"
  )
  val eabPropertyOmbudsman: Eab       = Eab(completeRedressScheme, hasAccepted = true)

  val completeRedressSchemePropertyRedress: JsObject = Json.obj(
    "redressScheme" -> "propertyRedressScheme"
  )
  val eabPropertyRedress: Eab                        = Eab(completeRedressSchemePropertyRedress, hasAccepted = true)

  val completeRedressSchemeOmbudsmanServices: JsObject = Json.obj(
    "redressScheme" -> "ombudsmanServices"
  )
  val eabOmbudsmanServices: Eab                        = Eab(completeRedressSchemeOmbudsmanServices, hasAccepted = true)

  val completeRedressSchemeOther: JsObject = Json.obj(
    "redressScheme" -> "other"
  )
  val eabOther: Eab                        = Eab(completeRedressSchemeOther, hasAccepted = true)

  val eabNoRedress: Eab = Eab(Json.obj(), hasAccepted = true)

  val accountantNameCompleteModel: Option[BusinessActivities] = Some(
    BusinessActivities(
      whoIsYourAccountant = Some(
        WhoIsYourAccountant(
          Some(WhoIsYourAccountantName("Accountant name", None)),
          Some(WhoIsYourAccountantIsUk(true)),
          Some(UkAccountantsAddress("", None, None, None, ""))
        )
      )
    )
  )

  val accountantNameInCompleteModel: Option[BusinessActivities] = Some(BusinessActivities(whoIsYourAccountant = None))

  val completeRpAndNotNominated: ResponsiblePerson = completeResponsiblePerson.copy(positions =
    Some(Positions(Set(BeneficialOwner), Some(PositionStartDate(LocalDate.now()))))
  )

  val inCompleteRpAndNominated: ResponsiblePerson =
    completeResponsiblePerson.copy(approvalFlags = ApprovalFlags(None, None))

  val inCompleteRpAndNotNominated: ResponsiblePerson = completeResponsiblePerson.copy(
    personName = None,
    positions = Some(Positions(Set(BeneficialOwner), Some(PositionStartDate(LocalDate.now()))))
  )

  val oneCompleteNominatedOff: Option[Seq[ResponsiblePerson]] =
    Some(
      Seq(
        completeResponsiblePerson,
        completeRpAndNotNominated,
        incompleteResponsiblePeople,
        inCompleteRpAndNotNominated
      )
    )

  val oneInCompleteNominatedOff: Option[Seq[ResponsiblePerson]] =
    Some(
      Seq(inCompleteRpAndNominated, completeRpAndNotNominated, incompleteResponsiblePeople, inCompleteRpAndNotNominated)
    )

  val inCompleteAndNoNominatedOff: Option[Seq[ResponsiblePerson]] =
    Some(Seq(inCompleteRpAndNotNominated, incompleteResponsiblePeople))

  "ControllerHelper" must {

    "hasIncompleteResponsiblePerson" must {

      "return false" when {
        "responsiblePerson seq is None" in {
          ControllerHelper.hasIncompleteResponsiblePerson(None) mustEqual false
        }

        "all responsiblePerson are complete" in {
          ControllerHelper.hasIncompleteResponsiblePerson(createCompleteResponsiblePersonSeq) mustEqual false
        }
      }

      "return true" when {
        "any responsiblePerson is not complete" in {
          ControllerHelper.hasIncompleteResponsiblePerson(createRPsWithMissingDoB) mustEqual true
        }
      }
    }

    "anotherBodyComplete" must {
      "return tuple of (is anotherBodyComplete, is anotherBodyYes) for AnotherBodyNo " in {
        val supervision = Supervision(Some(AnotherBodyNo))

        ControllerHelper.anotherBodyComplete(supervision) mustBe Some((true, false))
      }

      "return tuple of (is anotherBodyComplete, is anotherBodyYes) for AnotherBodyYes " in {
        val supervision = Supervision(Some(AnotherBodyYes(supervisorName = "Name")))

        ControllerHelper.anotherBodyComplete(supervision) mustBe Some((false, true))
      }

      "return tuple of (is anotherBodyComplete, is anotherBodyYes) for complete AnotherBodyYes " in {
        val supervision = Supervision(
          Some(
            AnotherBodyYes(
              "Name",
              Some(SupervisionStart(LocalDate.of(1990, 2, 24))),
              Some(SupervisionEnd(LocalDate.of(1998, 2, 24))),
              Some(SupervisionEndReasons("Reason"))
            )
          )
        )

        ControllerHelper.anotherBodyComplete(supervision) mustBe Some((true, true))
      }
    }

    "isAnotherBodyYes" must {
      "return true if AnotherBody is instance of AnotherBodyYes" in {
        val supervision = Supervision(Some(AnotherBodyYes(supervisorName = "Name")))

        ControllerHelper.isAnotherBodyYes(ControllerHelper.anotherBodyComplete(supervision)) mustBe true
      }

      "return false if AnotherBody is AnotherBodyNo" in {
        val supervision = Supervision(Some(AnotherBodyNo))

        ControllerHelper.isAnotherBodyYes(ControllerHelper.anotherBodyComplete(supervision)) mustBe false
      }
    }

    "isAnotherBodyComplete" must {
      "return true if AnotherBodyYes is complete" in {
        val supervision = Supervision(
          Some(
            AnotherBodyYes(
              "Name",
              Some(SupervisionStart(LocalDate.of(1990, 2, 24))),
              Some(SupervisionEnd(LocalDate.of(1998, 2, 24))),
              Some(SupervisionEndReasons("Reason"))
            )
          )
        )

        ControllerHelper.isAnotherBodyComplete(ControllerHelper.anotherBodyComplete(supervision)) mustBe true
      }

      "return false if AnotherBodyYes is incomplete" in {
        val supervision = Supervision(
          Some(
            AnotherBodyYes(
              "Name",
              Some(SupervisionStart(LocalDate.of(1990, 2, 24))),
              Some(SupervisionEnd(LocalDate.of(1998, 2, 24)))
            )
          )
        )

        ControllerHelper.isAnotherBodyComplete(ControllerHelper.anotherBodyComplete(supervision)) mustBe false
      }

      "return true if AnotherBody is AnotherBodyNo" in {
        val supervision = Supervision(Some(AnotherBodyNo))

        ControllerHelper.isAnotherBodyComplete(ControllerHelper.anotherBodyComplete(supervision)) mustBe true
      }
    }

    "return accountant name" when {
      "accountant name is called with complete model" in {
        ControllerHelper.accountantName(accountantNameCompleteModel) mustEqual "Accountant name"
      }
    }

    "return empty string" when {
      "accountant name is called with incomplete model" in {
        ControllerHelper.accountantName(accountantNameInCompleteModel) mustEqual ""
      }
    }

    "supervisionComplete" must {
      "return false if supervision section is incomplete" in {
        ControllerHelper.supervisionComplete(mockCacheMap) mustBe false
      }

      "return true if supervision section is complete" in {
        val complete = mock[Supervision]

        when(complete.isComplete) thenReturn true
        when(mockCacheMap.getEntry[Supervision]("supervision")) thenReturn Some(complete)

        ControllerHelper.supervisionComplete(mockCacheMap) mustBe true
      }

      "return false if supervision section is not available" in {
        val complete = mock[Supervision]

        when(complete.isComplete) thenReturn false
        when(mockCacheMap.getEntry[Supervision]("supervision")) thenReturn None

        ControllerHelper.supervisionComplete(mockCacheMap) mustBe false
      }
    }

    "isAbComplete" must {

      val start  = Some(SupervisionStart(LocalDate.of(1990, 2, 24))) // scalastyle:off magic.number
      val end    = Some(SupervisionEnd(LocalDate.of(1998, 2, 24))) // scalastyle:off magic.number
      val reason = Some(SupervisionEndReasons("Reason"))

      "return true if another body is AnotherBodyNo" in {
        ControllerHelper.isAbComplete(AnotherBodyNo) mustBe true
      }

      "return true if another body is complete AnotherBodyYes" in {
        ControllerHelper.isAbComplete(AnotherBodyYes("Supervisor", start, end, reason)) mustBe true
      }

      "return false if another body is incomplete AnotherBodyYes" in {
        ControllerHelper.isAbComplete(AnotherBodyYes("Supervisor", None, end, reason)) mustBe false
      }
    }

    "hasCompleteNominatedOfficer" must {
      "return true if there is complete nominated officer in the cache" in {
        ControllerHelper.hasCompleteNominatedOfficer(oneCompleteNominatedOff) mustBe true
      }

      "return false if there is incomplete nominated officer in the cache" in {
        ControllerHelper.hasCompleteNominatedOfficer(oneInCompleteNominatedOff) mustBe false
      }

      "return false if there are incomplete rps in the cache" in {
        ControllerHelper.hasCompleteNominatedOfficer(inCompleteAndNoNominatedOff) mustBe false
      }
    }

    "completeNominatedOfficerTitleName" must {
      "return officer name if there is complete nominated officer in the cache" in {
        ControllerHelper.completeNominatedOfficerTitleName(oneCompleteNominatedOff) mustBe Some("ANSTY DAVID")
      }

      "return empty string if there is incomplete nominated officer in the cache" in {
        ControllerHelper.completeNominatedOfficerTitleName(oneInCompleteNominatedOff) mustBe Some("")
      }

      "return empty string if there are incomplete rps in the cache" in {
        ControllerHelper.completeNominatedOfficerTitleName(inCompleteAndNoNominatedOff) mustBe Some("")
      }
    }

    "getNominatedOfficer" must {
      "return the nominated officer when there is one" in {
        ControllerHelper.getNominatedOfficer(oneCompleteNominatedOff.get) mustBe Some(completeResponsiblePerson)
      }

      "not return the nominated officer when there isn't one" in {
        ControllerHelper.getNominatedOfficer(Seq(completeRpAndNotNominated, inCompleteRpAndNotNominated)) mustBe None
      }
    }

    "getCompleteNominatedOfficer" must {
      "return the nominated officer" when {
        "the responsible person is a nominated officer and is complete" in {
          val vatRegisteredCompleteNominatedPerson = completeResponsiblePerson.copy(
            soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true))
          )

          ControllerHelper.getCompleteNominatedOfficer(
            Seq(completeRpAndNotNominated, inCompleteRpAndNominated, vatRegisteredCompleteNominatedPerson)
          ) mustBe Some(vatRegisteredCompleteNominatedPerson)
        }
      }

      "return no nominated officer" when {
        "there is no nominated officer" in {
          ControllerHelper.getCompleteNominatedOfficer(
            Seq(completeRpAndNotNominated, inCompleteRpAndNotNominated)
          ) mustBe None
        }

        "there is no complete responsible person" ignore {
          val nominatedButNotCompleteResponsiblePerson = completeResponsiblePerson.copy(
            positions = completeResponsiblePerson.positions.map(pos => pos.copy(startDate = None)),
            soleProprietorOfAnotherBusiness = None,
            addressHistory = None
          )

          ControllerHelper.getCompleteNominatedOfficer(Seq(nominatedButNotCompleteResponsiblePerson))
        }
      }
    }
  }
}
