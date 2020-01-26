using System;
using System.Collections.Generic;
using System.Data;
using System.Linq;
using System.Web;

namespace MM_Events.Controls
{
    public static class OutsourceRequestControl
    {
        public static void SubmitRequest(int requestId, bool approved)
        {
            Data_Utilities.SetResponsibleForRequest(requestId, "");
            if (approved)
                Data_Utilities.SetRequestStatus(requestId, "APPROVED");
            else
                Data_Utilities.SetRequestStatus(requestId, "REJECTED");
        }

        private static DataRow GetTaskForFinancialRequest(int requestId)
        {
            return Data_Utilities.GetTaskForRequest(requestId);
        }
    }
}