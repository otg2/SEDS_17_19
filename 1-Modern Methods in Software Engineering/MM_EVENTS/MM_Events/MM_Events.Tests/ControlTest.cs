using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Data;
using MM_Events.Controls;

namespace MM_Events.Tests
{
    [TestClass]
    public class ControlTest
    {
        DataRow request1;
        DataRow request2;
        DataRow request3;
        DataRow request4;
        DataRow request5;

        [TestInitialize]
        public void StartUp()
        {
            var request = new DataTable();
            request.Columns.Add("ReqId");
            request.Columns.Add("ReqType");
            request.Columns.Add("ReqResp");
            request.Columns.Add("ReqDescr");
            request.Columns.Add("ReqDate");
            request.Columns.Add("ReqBudget");
            request.Columns.Add("ReqTaskId");
            request.Columns.Add("ReqStatus");

            request.Rows.Add(new object[] { 1, "EVENT", "SCS", "A nice event", new DateTime(2017, 11, 10), 5000, 12, "READY" });
            request.Rows.Add(new object[] { 1, "EVENT", "PM", "A nice event", new DateTime(2017, 11, 10), 5000, 12, "READY" });
            request.Rows.Add(new object[] { 1, "EVENT", "SCS", "A nice event", new DateTime(2017, 11, 10), 5000, 12, "OPEN" });
            request.Rows.Add(new object[] { 1, "EVENT", null, "A nice event", new DateTime(2017, 11, 10), 5000, 12, "READY" });
            request.Rows.Add(new object[] { 1, "EVENT", "SCS", "A nice event", new DateTime(2017, 11, 10), 5000, 12, null });

            request1 = request.Rows[0];
            request2 = request.Rows[1];
            request3 = request.Rows[2];
            request4 = request.Rows[3];
            request5 = request.Rows[4];
        }

        [TestMethod]
        public void ShouldAccept_HappyPath()
        {
            var output = EventRequestControl.ShouldAccept(request1);
            Assert.IsTrue(output);
        }

        [TestMethod]
        public void ShouldAccept_WrongResp_ShouldFail()
        {
            var output = EventRequestControl.ShouldAccept(request2);
            Assert.IsFalse(output);
        }

        [TestMethod]
        public void ShouldAccept_WrongStatus_ShouldFail()
        {
            var output = EventRequestControl.ShouldAccept(request3);
            Assert.IsFalse(output);
        }

        [TestMethod]
        [ExpectedException(typeof(NoNullAllowedException))]
        public void ShouldAccept_RespNull_ThrowsError()
        {
            var output = EventRequestControl.ShouldAccept(request4);
        }

        [TestMethod]
        [ExpectedException(typeof(NoNullAllowedException))]
        public void ShouldAccept_StatusNull_ThrowsError()
        {
            var output = EventRequestControl.ShouldAccept(request5);
            Assert.IsTrue(output);
        }

        [TestMethod]
        public void GetNextReceiver_CurrentSCS_ShouldReturnFM()
        {
            var currentResponsible = "SCS";
            var nextResponsible = EventRequestControl.GetNextReceiver(currentResponsible);
            Assert.AreEqual(nextResponsible, "FM");
        }

        [TestMethod]
        public void GetNextReceiver_CurrentFM_ShouldReturnADM()
        {
            var currentResponsible = "FM";
            var nextResponsible = EventRequestControl.GetNextReceiver(currentResponsible);
            Assert.AreEqual(nextResponsible, "ADM");
        }

        [TestMethod]
        public void GetNextReceiver_CurrentADM_ShouldReturnSCS()
        {
            var currentResponsible = "ADM";
            var nextResponsible = EventRequestControl.GetNextReceiver(currentResponsible);
            Assert.AreEqual(nextResponsible, "SCS");
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void GetNextReceiver_CurrentEmpty_ShouldThrowException()
        {
            var currentResponsible = "";
            var nextResponsible = EventRequestControl.GetNextReceiver(currentResponsible);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void ShouldAccept_CurrentEmpty_ShouldThrowException()
        {
            var currentResponsible = "";
            var nextResponsible = EventRequestControl.GetNextReceiver(currentResponsible);
        }

        [TestMethod]
        public void GetNextTaskStatus_CurrentStatusPendingNoBudget_ShouldReturnInProgress()
        {
            var status = "PENDING";
            var budget = 0m;
            var expectedStatus = "IN PROGRESS";
            var nextStatus = TaskControl.GetNextTaskStatus(status, budget);
            Assert.AreEqual(nextStatus, expectedStatus);
        }

        [TestMethod]
        public void GetNextTaskStatus_CurrentStatusPendingWithBudget_ShouldReturnPendingFinancialRequest()
        {
            var status = "PENDING";
            var budget = 500m;
            var expectedStatus = "PENDING FINANCIAL REQUEST";
            var nextStatus = TaskControl.GetNextTaskStatus(status, budget);
            Assert.AreEqual(nextStatus, expectedStatus);
        }

        [TestMethod]
        public void GetNextTaskStatus_CurrentStatusInProgress_ShouldReturnFinished()
        {
            var status = "IN PROGRESS";
            var budget = 0m;
            var expectedStatus = "FINISHED";
            var nextStatus = TaskControl.GetNextTaskStatus(status, budget);
            Assert.AreEqual(nextStatus, expectedStatus);
        }

        [TestMethod]
        public void GetNextTaskStatus_CurrentStatusPendingFinancialRequest_ShouldReturnPending()
        {
            var status = "PENDING FINANCIAL REQUEST";
            var budget = 0m;
            var expectedStatus = "PENDING";
            var nextStatus = TaskControl.GetNextTaskStatus(status, budget);
            Assert.AreEqual(nextStatus, expectedStatus);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentNullException))]
        public void GetNextTaskStatus_CurrentStatusNull_ShouldThrowError()
        {
            var budget = 0m;
            var expectedStatus = "IN PROGRESS";
            var nextStatus = TaskControl.GetNextTaskStatus(null, budget);
            Assert.AreEqual(nextStatus, expectedStatus);
        }
    }
}
