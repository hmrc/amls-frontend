package amls


import org.scalatest.Suite

import play.api.test.{ FakeApplication}


import uk.gov.hmrc.play.test.WithFakeApplication


trait FakeAmlsApp extends WithFakeApplication {
  this: Suite =>

  override lazy val fakeApplication = FakeApplication()

}