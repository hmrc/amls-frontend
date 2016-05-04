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
    private val start = new LocalDate(1993, 8, 25) //scalastyle:off magic.number
    private val end = new LocalDate(1999, 8, 25)   //scalastyle:off magic.number
    private val reason = "Ending reason"

    val DefaultAnotherBody = AnotherBodyYes(supervisor, start, end, reason)

  }

  object NewValues {
    val NewAnotherBody = AnotherBodyNo
  }

  val completeModel = Supervision(Some(DefaultValues.DefaultAnotherBody))

  val completeJson = Json.obj(
    "anotherBody" -> Json.obj(
      "anotherBody" -> true,
      "supervisorName" -> "Company A",
      "startDate" -> "1993-08-25",
      "endDate" -> "1999-08-25",
      "endingReason" -> "Ending reason"
    )
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

        val notStartedSection = Section("supervision", NotStarted, controllers.routes.RegistrationProgressController.get())

        when(cache.getEntry[Supervision]("supervision")) thenReturn None

        Supervision.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Supervision]
        val completedSection = Section("supervision", Completed, controllers.routes.RegistrationProgressController.get())

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Supervision]("supervision")) thenReturn Some(complete)

        Supervision.section must be(completedSection)

      }

      "return a Started Section when model is incomplete" in {

        val incomplete = mock[Supervision]
        val startedSection = Section("supervision", Started, controllers.routes.RegistrationProgressController.get())

        when(incomplete.isComplete) thenReturn false
        when(cache.getEntry[Supervision]("supervision")) thenReturn Some(incomplete)

        Supervision.section must be(startedSection)

      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in {
        completeModel.isComplete must be(true)
      }
    }

    "Complete Model" when {

      "correctly convert between json formats" when {

        "Serialise as expected" in {
          Json.toJson(completeModel) must be(completeJson)
        }

        "Deserialise as expected" in {
          completeJson.as[Supervision] must be(completeModel)
        }
      }
    }


  }

}
