package models.supervision

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import models.tcsp.Tcsp
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


trait SupervisionValues {

  object DefaultValues {

    private val supervisor = "Company A"
    private val start = new LocalDate(1993, 8, 25)
    //scalastyle:off magic.number
    private val end = new LocalDate(1999, 8, 25)
    //scalastyle:off magic.number
    private val reason = "Ending reason"

    val DefaultAnotherBody = AnotherBodyYes(supervisor, start, end, reason)
    val DefaultProfessionalBody = ProfessionalBodyYes("details")
    val DefaultProfessionalBodyMember = ProfessionalBodyMemberYes(Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("test")))
  }

  object NewValues {
    val NewAnotherBody = AnotherBodyNo
    val NewProfessionalBody = ProfessionalBodyNo
    val ProfessionalBodyMemberYes = ProfessionalBodyMemberNo
  }

  val completeModel = Supervision(
    Some(DefaultValues.DefaultAnotherBody),
    Some(DefaultValues.DefaultProfessionalBodyMember),
    Some(DefaultValues.DefaultProfessionalBody))
  val partialModel = Supervision(Some(DefaultValues.DefaultAnotherBody))

  val completeJson = Json.obj(
    "anotherBody" -> Json.obj(
      "anotherBody" -> true,
      "supervisorName" -> "Company A",
      "startDate" -> "1993-08-25",
      "endDate" -> "1999-08-25",
      "endingReason" -> "Ending reason"
    ),
    "professionalBodyMember" -> Json.obj(
      "isAMember" -> true,
      "businessType" -> Json.arr("01", "02", "14"),
      "specifyOtherBusiness" -> "test"
    ),
    "professionalBody" -> Json.obj(
      "penalised" -> true,
      "professionalBody" -> "details"
    ),
    "hasChanged" -> false
  )
}

class SupervisionSpec extends PlaySpec with MockitoSugar with SupervisionValues {

  "Supervision" must {

    "have a default function that" must {

      "correctly provides a default value when none is provided" in {
        Supervision.default(None) must be(Supervision())
      }

      "correctly provides a default value when existing value is provided" in {
        Supervision.default(Some(completeModel)) must be(completeModel)
      }

    }

    "have a mongo key that" must {
      "be correctly set" in {
        Supervision.mongoKey() must be("supervision")
      }
    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {

        val notStartedSection = Section("supervision", NotStarted, false,  controllers.supervision.routes.WhatYouNeedController.get())

        when(cache.getEntry[Supervision]("supervision")) thenReturn None

        Supervision.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Supervision]
        val completedSection = Section("supervision", Completed, false,  controllers.supervision.routes.SummaryController.get(true))

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Supervision]("supervision")) thenReturn Some(complete)

        Supervision.section must be(completedSection)

      }

      "return a Started Section when model is incomplete" in {

        val incomplete = mock[Supervision]
        val startedSection = Section("supervision", Started, false,  controllers.supervision.routes.WhatYouNeedController.get())

        when(incomplete.isComplete) thenReturn false
        when(cache.getEntry[Supervision]("supervision")) thenReturn Some(incomplete)

        Supervision.section must be(startedSection)

      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeModel.isComplete must be(true)
      }

      "correctly show if the model is incomplete" in {
        partialModel.isComplete must be(false)
      }
    }

    "Complete Model" when {

      "correctly convert between json formats" when {

        "Serialise as expected" in {
          Json.toJson(completeModel) must be(completeJson)
        }

        "Deserialise as expected" in {
          (completeJson - "hasChanged").as[Supervision] must be(completeModel)
        }
      }
    }

    "Supervision with all values set as None" must {
      val initial: Option[Supervision] = None

      "return Supervision with anotherBody set and indicating changes have been made" in {
        val result = initial.anotherBody(DefaultValues.DefaultAnotherBody)
        result must be(Supervision(Some(DefaultValues.DefaultAnotherBody), None, hasChanged = true))
      }
      "return Supervision with professionalBody set and indicating changes have been made" in {
        val result = initial.professionalBody(DefaultValues.DefaultProfessionalBody)
        result must be(Supervision(professionalBody = Some(DefaultValues.DefaultProfessionalBody), hasChanged = true))
      }
      "return Supervision with professionalBodyMember set and indicating changes have been made" in {
        val result = initial.professionalBodyMember(DefaultValues.DefaultProfessionalBodyMember)
        result must be(Supervision(professionalBodyMember = Some(DefaultValues.DefaultProfessionalBodyMember), hasChanged = true))
      }

    }
    "Supervision with existing values" must {
      val initial: Supervision = completeModel

      "return Supervision with anotherBody set and indicating changes have been made" in {
        val result = initial.anotherBody(NewValues.NewAnotherBody)
        result must be(completeModel.copy(Some(NewValues.NewAnotherBody), hasChanged = true))
      }
      "return Supervision with professionalBody set and indicating changes have been made" in {
        val result = initial.professionalBody(NewValues.NewProfessionalBody)
        result must be(completeModel.copy(professionalBody = Some(NewValues.NewProfessionalBody), hasChanged = true))
      }
      "return Supervision with professionalBodyMember set and indicating changes have been made" in {
        val result = initial.professionalBodyMember(NewValues.ProfessionalBodyMemberYes)
        result must be(completeModel.copy(professionalBodyMember = Some(NewValues.ProfessionalBodyMemberYes), hasChanged = true))
      }
    }

    "Supervision class" when {
      "anotherBody value is set" which {
        "is the same as before" must {
          "leave the object unchanged" in {
            val res: Supervision = completeModel.anotherBody(DefaultValues.DefaultAnotherBody)
            res must be(completeModel)
            res.hasChanged must be(false)
          }
        }

        "is different" must {
          "set the hasChanged & previouslyRegisterd Properties" in {
            val res = completeModel.anotherBody(NewValues.NewAnotherBody)
            res.hasChanged must be(true)
            res.anotherBody must be(Some(NewValues.NewAnotherBody))
          }
        }
      }

      "professionalBodyMember value is set" which {
        "is the same as before" must {
          "leave the object unchanged" in {
            val res: Supervision = completeModel.professionalBodyMember(DefaultValues.DefaultProfessionalBodyMember)
            res must be(completeModel)
            res.hasChanged must be(false)
          }
        }
        "is different" must {
          "set the hasChanged & previouslyRegisterd Properties" in {
            val res = completeModel.professionalBodyMember(NewValues.ProfessionalBodyMemberYes)
            res.hasChanged must be(true)
            res.professionalBodyMember must be(Some(NewValues.ProfessionalBodyMemberYes))
          }
        }
      }

      "professionalBody value is set" which {
        "is the same as before" must {
          "leave the object unchanged" in {
            val res: Supervision = completeModel.professionalBody(DefaultValues.DefaultProfessionalBody)
            res must be(completeModel)
            res.hasChanged must be(false)
          }
        }
        "is different" must {
          "set the hasChanged & previouslyRegisterd Properties" in {
            val res = completeModel.professionalBody(NewValues.NewProfessionalBody)
            res.hasChanged must be(true)
            res.professionalBody must be(Some(NewValues.NewProfessionalBody))
          }
        }
      }
    }
  }
}


