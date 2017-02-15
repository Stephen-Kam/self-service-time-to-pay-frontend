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

package uk.gov.hmrc.selfservicetimetopay.controllers

import java.time.LocalDate

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.models.{Debit, EligibilityExistingTTP, EligibilityTypeOfTax}
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class CalculatorControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with WithFakeApplication with BeforeAndAfterEach {

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockCalculatorConnector: CalculatorConnector = mock[CalculatorConnector]

  val controller = new CalculatorController(mockCalculatorConnector) {
    override lazy val sessionCache = mockSessionCache
    override lazy val authConnector = mockAuthConnector
  }

  override def beforeEach() {
    reset(mockAuthConnector, mockSessionCache, mockCalculatorConnector)
  }

  "CalculatorControllerSpec" should {
    "Return OK for non-logged-in calculation submission" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLI)))

      val result = await(controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.OK
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "if no schedule is present, generate a schedule and populate it in the session" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLI.copy(schedule = None))))

      when(mockCalculatorConnector.calculatePaymentSchedule(any())(any()))
        .thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.CalculatorController.getCalculateInstalments(None).url
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return BadRequest if the form value = total amount due" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(eligibilityTypeOfTax = Some(EligibilityTypeOfTax(true, false)), eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))), calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now()))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "300.00")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return BadRequest if the form value = total amount due (via rounding)" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(eligibilityTypeOfTax =
        Some(EligibilityTypeOfTax(true, false)),
        eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
        calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now()))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "299.999")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return 303 if the form value < total amount due (via rounding)" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(eligibilityTypeOfTax =
        Some(EligibilityTypeOfTax(true, false)),
        eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
        calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now()))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      when(mockCalculatorConnector.calculatePaymentSchedule(any())(any()))
        .thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "299.994")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return 303 for non-logged-in when schedule is missing" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLINoSchedule)))

      when(mockCalculatorConnector.calculatePaymentSchedule(any())(any()))
        .thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.CalculatorController.getCalculateInstalments(None).url
    }

    "Return 303 for non-logged-in when TTPSubmission is missing for submitPaymentToday" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully display the what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(controller.getDebitDate().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      bodyOf(result) should include(Messages("ssttp.calculator.form.what-you-owe-date.example"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully redirect to the start page when missing submission data for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = await(controller.getDebitDate().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.SelfServiceTimeToPayController.start().url
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully submit the what you owe date page with valid form data" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])


      val result = await(controller.submitDebitDate().apply(FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "2017",
          "dueBy.dueByMonth" -> "1",
          "dueBy.dueByDay" -> "31")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.CalculatorController.getAmountOwed().url
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit invalid year lower than minimum value in form data and return errors for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(controller.submitDebitDate().apply(FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "1900",
          "dueBy.dueByMonth" -> "1",
          "dueBy.dueByDay" -> "31")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include(Messages("ssttp.calculator.form.what-you-owe-date.due_by.not-valid-year-too-low"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit invalid year greater than maximum value in form data and return errors for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(controller.submitDebitDate().apply(FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "2111",
          "dueBy.dueByMonth" -> "1",
          "dueBy.dueByDay" -> "31")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include(Messages("ssttp.calculator.form.what-you-owe-date.due_by.not-valid-year-too-high"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit invalid month in form data and return errors for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(controller.submitDebitDate().apply(FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "2017",
          "dueBy.dueByMonth" -> "13",
          "dueBy.dueByDay" -> "31")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include(Messages("ssttp.calculator.form.what-you-owe.due_by.not-valid-month"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit invalid day in form data and return errors for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(controller.submitDebitDate().apply(FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "2017",
          "dueBy.dueByMonth" -> "1",
          "dueBy.dueByDay" -> "32")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include(Messages("ssttp.calculator.form.what-you-owe-date.due_by.not-valid-day"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully display the what you owe amount page" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      val result = await(controller.getAmountOwed().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      bodyOf(result) should include(Messages("ssttp.calculator.form.what-you-owe-amount.title1"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully redirect to the start page when missing submission data for what you owe amount page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(controller.getAmountOwed().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.SelfServiceTimeToPayController.start().url
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully submit the what you owe amount page with valid form data" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      val result = await(controller.submitAmountOwed().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "5000")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.CalculatorController.getAmountsDue().url
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit amount less than 0 in form data for the what you owe amount page and return errors" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      val result = await(controller.submitAmountOwed().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "-500")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include(Messages("ssttp.calculator.form.what-you-owe-amount.amount.required"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit empty amount in form data for the what you owe amount page and return errors" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      val result = await(controller.submitAmountOwed().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include(Messages("ssttp.calculator.form.what-you-owe-amount.amount.required"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

  }
}
