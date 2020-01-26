using System;
using System.Data;

namespace MM_Events.Controls
{
    public static class EventRequestControl
    {
        public static void SubmitRequest(int requestId)
        {
            var _request = GetRequestForId(requestId);

            if (ShouldAccept(_request))
                HandleAcceptRequest(requestId);
            else
            {
                var _sendTo = GetNextReceiver(_request["ReqResp"] as string);

                if (_sendTo == "SCS")
                {
                    PrepareEvent(requestId);
                }

                SetResponsibleForRequest(requestId, _sendTo);
            }
        }

        public static void SubmitRequest(int requestId, decimal budget, string financialComment)
        {
            SubmitRequest(requestId);
            SaveFinancialComment(requestId, financialComment);
            SaveFinancialBudget(requestId, budget);
        }

        public static void CancelRequest(int requestId)
        {
            CloseEventRequest(requestId);
            CancelEvent(requestId);
        }

        private static void PrepareEvent(int requestId)
        {
            Data_Utilities.SetEventStatusToReady(requestId);
        }

        private static void SaveFinancialBudget(int requestId, decimal budget)
        {
            Data_Utilities.SaveFinancialBudget(requestId, budget);
        }

        private static void SaveFinancialComment(int requestId, string financialComment)
        {
            Data_Utilities.SaveFinancialComment(requestId, financialComment);
        }

        private static void HandleAcceptRequest(int requestId)
        {
            AcceptEvent(requestId);
            CloseEventRequest(requestId);
        }

        private static void CloseEventRequest(int requestId)
        {
            Data_Utilities.setEventRequestToClosed(requestId);
        }

        private static void AcceptEvent(int requestId)
        {
            Data_Utilities.setEventStatusToAccepted(requestId);
        }

        public static bool ShouldAccept(DataRow request)
        {
            var responsible = request["ReqResp"] as string;
            var status = request["ReqStatus"] as string;

            if (responsible == null || status == null)
                throw new NoNullAllowedException("Either person responsible or request status is null");

            return (string)request["ReqResp"] == "SCS" && (string)request["ReqStatus"] == "READY";
        }

        private static void SetResponsibleForRequest(int requestId, string sendTo)
        {
            Data_Utilities.SetResponsibleForRequest(requestId, sendTo);
        }

        private static DataRow GetRequestForId(int requestId)
        {
            return Data_Utilities.GetRequest(requestId);
        }

        private static void CancelEvent(int requestId)
        {
            Data_Utilities.CancelEvent(requestId);
        }

        public static string GetNextReceiver(string responsible)
        {
            switch (responsible)
            {
                case "SCS":
                    return "FM";
                case "FM":
                    return "ADM";
                case "ADM":
                    return "SCS";
                default:
                    throw new ArgumentException("Responsible party {0} not implemented for event task", responsible);
            }
        }
    }
}