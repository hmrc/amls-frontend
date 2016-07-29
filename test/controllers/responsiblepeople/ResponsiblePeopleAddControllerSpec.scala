package controllers.responsiblepeople

import java.util.concurrent.TimeUnit

import connectors.DataCacheConnector
import controllers.bankdetails.BankAccountTypeController
import models.bankdetails.BankDetails
import models.responsiblepeople.ResponsiblePeople
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.{PropertyChecks, TableDrivenPropertyChecks}
import org.scalatest.{Pending, WordSpecLike}
import org.scalatestplus.play.{OneAppPerTest, OneAppPerSuite}
import org.specs2.reporter.SpecFailureAssertionFailedError
import play.api.mvc.Call
import sun.security.provider.VerificationProvider
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture
import play.api.test.Helpers._
import org.scalacheck.Gen

import scala.annotation.tailrec
import scala.concurrent.Future

class ResponsiblePeopleAddControllerSpec extends WordSpecLike
  with MustMatchers with MockitoSugar with ScalaFutures with OneAppPerSuite with PropertyChecks {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ResponsiblePeopleAddController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    @tailrec
    final def buildTestSequence(requiredCount: Int, acc: Seq[ResponsiblePeople] = Nil): Seq[ResponsiblePeople] = {
      require(requiredCount >= 0, "cannot build a sequence with negative elements")
      if (requiredCount == acc.size) {
        acc
      } else {
        buildTestSequence(requiredCount, acc :+ ResponsiblePeople())
      }
    }

    def guidanceOptions(currentCount: Int) = Table(
      ("guidanceRequested", "expectedRedirect"),
      (true, controllers.responsiblepeople.routes.WhatYouNeedController.get(currentCount + 1)),
      (false, controllers.responsiblepeople.routes.PersonNameController.get(currentCount + 1, false))
    )
  }

  "ResponsiblePeopleController" when {
    "get is called" should {
      "add empty bankdetails and redirect to the correct page" in new Fixture {
        val min = 0
        val max = 25
        val requiredSuccess =10


        val zeroCase = Gen.const(0)
        val reasonableCounts = for (n <- Gen.choose(min, max)) yield n
        val partitions = Seq (zeroCase, reasonableCounts)

        forAll(reasonableCounts, minSuccessful(requiredSuccess)) { currentCount: Int =>
          forAll(guidanceOptions(currentCount)) { (guidanceRequested: Boolean, expectedRedirect: Call) =>
            val testSeq  = buildTestSequence(currentCount)
            println(s"currentCount = $currentCount")

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
              (any(), any(), any())).thenReturn(Future.successful(Some(testSeq)))

            val resultF = controller.get(guidanceRequested)(request)

            status(resultF) must be(SEE_OTHER)
            redirectLocation(resultF) must be(Some(expectedRedirect.url))

            verify(controller.dataCacheConnector)
              .save[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key), meq(testSeq :+ ResponsiblePeople()))(any(), any(), any())

            reset(controller.dataCacheConnector)
          }
        }
      }
    }
  }
}
