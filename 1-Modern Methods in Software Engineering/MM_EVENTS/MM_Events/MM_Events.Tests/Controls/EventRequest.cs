using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Data;

namespace MM_Events.Tests.Controls
{
    [TestClass]
    public class EventRequest
    {

        DataRow _eventRequest1;

        [TestInitialize]
        public void StartUp()
        {

            DataTable dt = new DataTable();
            dt.Columns.Add("ReqId");
            dt.Columns.Add("ReqType");
            dt.Columns.Add("ReqResp");
            dt.Columns.Add("ReqDescr");
            dt.Columns.Add("ReqDate");
            dt.Columns.Add("ReqBudget");
            dt.Columns.Add("ReqTaskId");
            dt.Columns.Add("ReqStatus");

            dt.Rows.Add(new object[] { 1, "EVENT", "SC", "Very important event", new DateTime(2017,10,6), 10000.0m, 15, "OPEN"});

            _eventRequest1 = dt.Rows[0];
        }

        [TestMethod]
        public void ShouldAccept_ADMResponsible_ShouldReturnFalse()
        {
        }
    }
}
