using System;
using System.Data;

namespace MM_Events.Controls
{
    public static class FinancialRequestControl
    {
        public static void SubmitFinancialRequest(int requestId, bool approved)
        {
            var task = GetTaskForFinancialRequest(requestId);
            var responsible = task["TaskTeam"] as string;
            var taskId = Convert.ToInt32(task["TaskId"]);
            var subteam = task["TaskStatusMsg"] as string;

            SubmitRequest(requestId, taskId, subteam, responsible, approved);
        }

        private static void SubmitRequest(int requestId, int taskId, string subteam, string responsible, bool approved)
        {
            Data_Utilities.SetResponsibleForRequest(requestId, responsible);
            if (approved)
            {
                Data_Utilities.SetRequestStatus(requestId, "APPROVED");
                Data_Utilities.SetTaskStatus(taskId, "IN PROGRESS");
                Data_Utilities.SetTaskResponsible(taskId, subteam);
            }
            else
            {
                Data_Utilities.SetRequestStatus(requestId, "REJECTED");
                Data_Utilities.SetTaskStatus(taskId, "CLOSED");
            }
        }

        private static DataRow GetTaskForFinancialRequest(int requestId)
        {
            return Data_Utilities.GetTaskForRequest(requestId);
        }
    }
}