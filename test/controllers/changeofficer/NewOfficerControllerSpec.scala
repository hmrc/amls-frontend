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

package controllers.changeofficer

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import generators.ResponsiblePersonGenerator
import models.changeofficer.{ChangeOfficer, NewOfficer, RoleInBusiness, SoleProprietor}
import models.responsiblepeople.ResponsiblePerson.flowChangeOfficer
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class NewOfficerControllerSpec extends AmlsSpec with ResponsiblePersonGenerator with PrivateMethodTester with ScalaFutures {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)

    val cache = mock[DataCacheConnector]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(cache))
      .build()

    lazy val controller = injector.instanceOf[NewOfficerController]

    lazy val responsiblePeople = Gen.listOf(responsiblePersonGen).sample.get
    lazy val emptyPerson = ResponsiblePerson()
    lazy val responsiblePeopleWithEmptyPerson = responsiblePeople :+ emptyPerson

    lazy val changeOfficer = ChangeOfficer(RoleInBusiness(Set(SoleProprietor)))

    when {
      cache.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any())
    } thenReturn Future.successful(Some(responsiblePeopleWithEmptyPerson))

    when {
      cache.fetch[ChangeOfficer](eqTo(ChangeOfficer.key))(any(), any(), any())
    } thenReturn Future.successful(Some(changeOfficer))
  }

  trait TestFixtureForChangeNominatedOfficer extends AuthorisedFixture {
    self =>
    val request = addToken(self.authRequest)

    val dataCacheConnector = mock[DataCacheConnector]
    val cacheMap = mock[CacheMap]

    val changeOfficer = ChangeOfficer(
      RoleInBusiness(Set.empty),
      Some(NewOfficer("NewOfficer"))
    )

    val newOfficer = ResponsiblePerson(
      personName = Some(PersonName("New", None, "Officer")),
      positions = Some(Positions(Set(
        DesignatedMember
      ), None)))

    val oldOfficer = ResponsiblePerson(
      personName = Some(PersonName("Old", None, "Officer")),
      positions = Some(Positions(Set(
        NominatedOfficer
      ), None)))

    val responsiblePeople = Seq(
      newOfficer,
      oldOfficer
    )

    lazy val responsiblePeopleGen = Gen.listOf(responsiblePersonGen).sample.get
    lazy val emptyPerson = ResponsiblePerson()
    lazy val responsiblePeopleWithEmptyPerson = responsiblePeopleGen :+ emptyPerson

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .build()

    when {
      controller.dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any())
    } thenReturn Future.successful(Some(responsiblePeople))

    when {
      cacheMap.getEntry[ChangeOfficer](eqTo(ChangeOfficer.key))(any())
    } thenReturn Some(changeOfficer)

    when {
      cacheMap.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
    } thenReturn Some(responsiblePeople)

    when {
      controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any())
    } thenReturn Future.successful(cacheMap)

    lazy val controller = injector.instanceOf[NewOfficerController]
  }

  trait TestFixtureForDeleteOldOfficer extends TestFixtureForChangeNominatedOfficer {
    self =>

    override val oldOfficer = ResponsiblePerson(
      personName = Some(PersonName("Old", None, "Officer")),
      positions = Some(Positions(Set(
        NominatedOfficer
      ), None)), endDate = Some(ResponsiblePersonEndDate(new LocalDate())), status = Some("Deleted"))

    when {
      cacheMap.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
    } thenReturn Some(Seq(
      newOfficer.copy(
        positions = Some(Positions(newOfficer.positions.get.positions + NominatedOfficer, newOfficer.positions.get.startDate)),
        hasChanged = true,
        hasAccepted = true
      )
    ))

    when {
      controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any())
    } thenReturn Future.successful(Some(Seq(
      newOfficer.copy(
        positions = Some(Positions(newOfficer.positions.get.positions + NominatedOfficer, newOfficer.positions.get.startDate)),
        hasChanged = true,
        hasAccepted = true
      )
    )))

    override lazy val controller = injector.instanceOf[NewOfficerController]
  }



  "The NewOfficerController" when {
    "get is called" must {
      "get the view and show all the responsible people, except people with no name" in new TestFixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        verify(cache).fetch(eqTo(ResponsiblePerson.key))(any(), any(), any())

        val html = Jsoup.parse(contentAsString(result))

        responsiblePeople.foreach { person =>
          html.select(s"input[type=radio][value=${person.personName.get.fullNameWithoutSpace}]").size() mustBe 1
        }
      }

      "prepopulate the view with the selected person" in new TestFixture {

        override lazy val responsiblePeople = Gen.listOfN(3, responsiblePersonGen).sample.get :+
          ResponsiblePerson(Some(PersonName("Test", None, "Person")))

        val model = ChangeOfficer(RoleInBusiness(Set(SoleProprietor)), Some(NewOfficer("TestPerson")))

        when {
          cache.fetch[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any(), any(), any())
        } thenReturn Future.successful(Some(responsiblePeople))

        when {
          cache.fetch[ChangeOfficer](eqTo(ChangeOfficer.key))(any(), any(), any())
        } thenReturn Future.successful(Some(model))

        val result = controller.get()(request)

        status(result) mustBe OK

        val html = Jsoup.parse(contentAsString(result))

        html.select("input[type=radio][value=TestPerson]").hasAttr("checked") mustBe true
      }

    }

    "post is called" must {
      "respond with SEE_OTHER and redirect to the RegistrationProgress controller" in new TestFixtureForChangeNominatedOfficer {

        when {
          dataCacheConnector.fetch[ChangeOfficer](any())(any(), any(), any())
        } thenReturn Future.successful(Some(ChangeOfficer(RoleInBusiness(Set(SoleProprietor)), None)))

        when {
          dataCacheConnector.save[ChangeOfficer](any(), any())(any(), any(), any())
        } thenReturn Future.successful(mock[CacheMap])

        val result = controller.post()(request.withFormUrlEncodedBody("person" -> "testName"))


        status(result) mustBe (SEE_OTHER)

        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

        verify(dataCacheConnector).save(
          eqTo(ChangeOfficer.key),
          eqTo(ChangeOfficer(
            RoleInBusiness(Set(models.changeofficer.SoleProprietor)),
            Some(NewOfficer("testName"))
          )))(any(), any(), any())

      }

      "respond with SEE_OTHER and redirect to the ResponsiblePeopleAddController" in new TestFixture {

        when {
          cache.fetch[ChangeOfficer](any())(any(), any(), any())
        } thenReturn Future.successful(Some(ChangeOfficer(RoleInBusiness(Set(SoleProprietor)), None)))

        when {
          cache.save[ChangeOfficer](any(), any())(any(), any(), any())
        } thenReturn Future.successful(mock[CacheMap])

        val result = controller.post()(request.withFormUrlEncodedBody("person" -> "someoneElse"))
        status(result) mustBe (SEE_OTHER)

        redirectLocation(result) mustBe Some(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(false, Some(flowChangeOfficer)).url)

      }

      "respond with BAD_REQUEST when invalid data is posted" in new TestFixture {

        val result = controller.post()(request)

        status(result) mustBe BAD_REQUEST

        val html = Jsoup.parse(contentAsString(result))

        responsiblePeople foreach { p =>
          contentAsString(result) must include(p.personName.get.fullName)
        }
      }

    }

    "getPeopleAndSelectedOfficer" must {

      "return all responsible people with name defined and without deleted status" in new TestFixture {

        override lazy val responsiblePeople = List(
          responsiblePersonGen.sample.get,
          responsiblePersonGen.sample.get.copy(personName = None),
          responsiblePersonGen.sample.get,
          responsiblePersonGen.sample.get.copy(status = Some(StatusConstants.Deleted)),
          responsiblePersonGen.sample.get
        )

        when {
          cache.fetch[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any(), any(), any())
        } thenReturn Future.successful(Some(responsiblePeople))

        val getPeopleAndSelectedOfficer = PrivateMethod[OptionT[Future, (NewOfficer, Seq[ResponsiblePerson])]]('getPeopleAndSelectedOfficer)

        val result = controller invokePrivate getPeopleAndSelectedOfficer(HeaderCarrier(), mock[AuthContext]) getOrElse fail("Could not retrieve")

        await(result) must equal((
          NewOfficer(""),
          Seq(
            responsiblePeople.head,
            responsiblePeople(2),
            responsiblePeople(4)
          )
        ))

      }
    }

    "addNominatedOfficer is called" must {
      "add NominatedOfficer to the positions of the given responsible person" in new TestFixtureForChangeNominatedOfficer {

        val addNominatedOfficer = PrivateMethod[ResponsiblePerson]('addNominatedOfficer)

        val result = controller invokePrivate addNominatedOfficer(newOfficer)

        result must equal(newOfficer.copy(
          positions = Some(Positions(Set(DesignatedMember, NominatedOfficer), newOfficer.positions.get.startDate)),
          hasChanged = true
        ))

      }
    }

    "removeNominatedOfficers is called" must {
      "remove NominatedOfficer from the positions of the given responsible person" in new TestFixtureForChangeNominatedOfficer {

        val removeNominatedOfficers = PrivateMethod[Seq[ResponsiblePerson]]('removeNominatedOfficers)

        val result = controller invokePrivate removeNominatedOfficers(responsiblePeople)

        result must equal(Seq(
          newOfficer,
          oldOfficer.copy(
            positions = Some(Positions(Set.empty[PositionWithinBusiness], oldOfficer.positions.get.startDate)),
            hasChanged = true
          )))
      }
    }

    "updateNominatedOfficers is called" must {
      "return a collection of responsible people with updated nominated officers" in new TestFixtureForChangeNominatedOfficer {

        val updateNominatedOfficers = PrivateMethod[Seq[ResponsiblePerson]]('updateNominatedOfficers)

        val result = controller invokePrivate updateNominatedOfficers((oldOfficer, 1), RoleInBusiness(Set()), responsiblePeople, 0)

        result must equal(Seq(
          newOfficer.copy(
            positions = Some(Positions(newOfficer.positions.get.positions + NominatedOfficer, newOfficer.positions.get.startDate)),
            hasChanged = true,
            hasAccepted = true
          ),
          oldOfficer.copy(
            positions = Some(Positions(oldOfficer.positions.get.positions - NominatedOfficer, oldOfficer.positions.get.startDate)),
            hasChanged = true,
            hasAccepted = true
          )
        ))
      }
    }

    "deleteOldOfficer is called" must {
      "return cache map without deleted rp (if rp was not submitted)" in new TestFixtureForDeleteOldOfficer {
        val deleteOldOfficer = PrivateMethod[Future[Product]]('deleteOldOfficer)

        val result = controller invokePrivate deleteOldOfficer(oldOfficer, 1, mock[AuthContext], HeaderCarrier())

        Await.result(result, 1 second) mustEqual cacheMap

        cacheMap.getEntry[ResponsiblePerson](ResponsiblePerson.key) must equal(Some(Seq(
          newOfficer.copy(
            positions = Some(Positions(newOfficer.positions.get.positions + NominatedOfficer, newOfficer.positions.get.startDate)),
            hasChanged = true,
            hasAccepted = true
          )
        )))
      }

      "return none if rp was submitted" in new TestFixtureForDeleteOldOfficer {
        override val oldOfficer = ResponsiblePerson(
          personName = Some(PersonName("Old", None, "Officer")),
          positions = Some(Positions(Set(
            NominatedOfficer
          ), None)), lineId = Some(11111))

        val deleteOldOfficer = PrivateMethod[Future[Product]]('deleteOldOfficer)

        val result = controller invokePrivate deleteOldOfficer(oldOfficer, 1, mock[AuthContext], HeaderCarrier())

        Await.result(result, 1 second) mustBe None
      }
    }
  }
}
