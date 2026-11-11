import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import axios from "axios";

// 직원
interface Employee {
  employeeId: number;
  employeeName: string;
  department: string;
}

// 휴가 목록
interface Leave {
  requestId: number;
  applicantName: string;
  approverName: string;
  startDate: string;
  endDate: string;
  reason: string;
  status: string;
}

// 휴가 상세
interface LeaveDetail extends Leave {
  employeeId: number;
  approverId: number;
  applicantDepartment: string;
  approverDepartment: string;
  createdAt: string;
  decidedAt: string | null;
  rejectionComment: string | null;
}

// 휴가 신청 응답
interface LeaveResponse {
  requestId: number;
  status: string;
}

// 페이징 응답
interface LeavePage {
  content: Leave[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// 검색 조건
interface LeaveSearch {
  employeeId?: number;
  approverId?: number;
  status?: string;
  sort: string;
  size: number;
}

// 처리 이력
interface LeaveHistory {
  historyId: number;
  actorName: string;
  actorDepartment: string;
  action: string;
  comment: string | null;
  actedAt: string;
}

// 목록 조회용 Axios 함수
function fetchLeavePage(search: LeaveSearch, page: number) {
  return axios.get<LeavePage>("/api/leaves", {
    params: { ...search, page },
  });
}

// 오류 메시지
function getErrorMessage(error: unknown) {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? "요청 실패";
  }
  return "요청 실패";
}

function App() {
  // 1. 직원
  const [employees, setEmployees] = useState<Employee[]>([]);

  // 2. 휴가 신청
  const [employeeId, setEmployeeId] = useState("");
  const [approverId, setApproverId] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 3. 휴가 목록·검색·페이징
  const [leavePage, setLeavePage] = useState<LeavePage | null>(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState("");

  const [search, setSearch] = useState<LeaveSearch>({
    sort: "newest",
    size: 5,
  });

  // 4. 신규: 휴가 상세 및 처리 이력
  const [detail, setDetail] = useState<LeaveDetail | null>(null);
  const [histories, setHistories] = useState<LeaveHistory[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");

  // 5. 신규: 반려 사유 및 처리 결과
  const [comment, setComment] = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const [acting, setActing] = useState(false);

  // 직원 목록 조회
  useEffect(() => {
    axios
      .get<Employee[]>("/api/employees")
      .then((res) => setEmployees(res.data))
      .catch(() => setMessage("직원 조회 실패"));
  }, []);

  // 휴가 목록 조회
  useEffect(() => {
    let active = true;

    setLoading(true);
    setListError("");

    fetchLeavePage(search, page)
      .then((res) => {
        if (active) setLeavePage(res.data);
      })
      .catch(() => {
        if (active) setListError("휴가 목록 조회 실패");
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [search, page]);

  // 신규: 목록 다시 조회
  async function refreshList() {
    try {
      const res = await fetchLeavePage(search, page);
      setLeavePage(res.data);
      setListError("");
    } catch {
      setListError("휴가 목록 조회 실패");
    }
  }

  // 휴가 신청
  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    if (employeeId === approverId) {
      setMessage("신청자와 승인자는 달라야 합니다.");
      return;
    }

    if (startDate > endDate) {
      setMessage("휴가 날짜를 확인하세요.");
      return;
    }

    setSubmitting(true);
    setMessage("");

    try {
      const res = await axios.post<LeaveResponse>("/api/leaves", {
        employeeId: Number(employeeId),
        approverId: Number(approverId),
        startDate,
        endDate,
        reason,
      });

      setMessage(`신청 완료! 번호: ${res.data.requestId}`);

      setEmployeeId("");
      setApproverId("");
      setStartDate("");
      setEndDate("");
      setReason("");

      await refreshList();
    } catch (error) {
      setMessage(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  // 검색
  function handleSearch(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    const form = new FormData(e.currentTarget);
    const employee = String(form.get("employeeId") ?? "");
    const approver = String(form.get("approverId") ?? "");

    setSearch({
      employeeId: employee ? Number(employee) : undefined,
      approverId: approver ? Number(approver) : undefined,
      status: String(form.get("status") ?? "") || undefined,
      sort: String(form.get("sort") ?? "newest"),
      size: Number(form.get("size") ?? 5),
    });

    setPage(1);
    setDetail(null);
  }

  // 신규: 휴가 상세 + 처리 이력 조회
  async function openDetail(requestId: number) {
    setDetailLoading(true);
    setDetailError("");
    setDetail(null);
    setHistories([]);
    setActionMessage("");

    try {
      const [leaveRes, historyRes] = await Promise.all([
        axios.get<LeaveDetail>(`/api/leaves/${requestId}`),
        axios.get<LeaveHistory[]>(`/api/leaves/${requestId}/history`),
      ]);

      setDetail(leaveRes.data);
      setHistories(historyRes.data);
    } catch (error) {
      setDetailError(getErrorMessage(error));
    } finally {
      setDetailLoading(false);
    }
  }

  // 신규: 승인·반려·취소
  async function handleAction(action: "approve" | "reject" | "cancel") {
    if (!detail || acting || detail.status !== "PENDING") return;

    if (action === "reject" && !comment.trim()) {
      setActionMessage("반려 사유를 입력하세요.");
      return;
    }

    const requestId = detail.requestId;

    setActing(true);
    setActionMessage("");

    try {
      if (action === "approve") {
        await axios.post(`/api/leaves/${requestId}/approve`, {
          approverId: detail.approverId,
        });
      } else if (action === "reject") {
        await axios.post(`/api/leaves/${requestId}/reject`, {
          approverId: detail.approverId,
          comment: comment.trim(),
        });
      } else {
        await axios.post(`/api/leaves/${requestId}/cancel`, {
          employeeId: detail.employeeId,
        });
      }

      // 변경된 데이터 다시 조회
      await openDetail(requestId);
      await refreshList();

      setComment("");
      setActionMessage("처리 완료");
    } catch (error) {
      setActionMessage(getErrorMessage(error));
    } finally {
      setActing(false);
    }
  }

  return (
    <div>
      <h1>Project3 휴가 관리 시스템</h1>

      {/* 직원 목록 */}
      <h2>직원 목록</h2>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>이름</th>
            <th>부서</th>
          </tr>
        </thead>
        <tbody>
          {employees.map((employee) => (
            <tr key={employee.employeeId}>
              <td>{employee.employeeId}</td>
              <td>{employee.employeeName}</td>
              <td>{employee.department}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <hr />

      {/* 휴가 신청 */}
      <h2>휴가 신청</h2>

      <form onSubmit={handleSubmit}>
        <p>
          신청자:
          <select
            value={employeeId}
            onChange={(e) => setEmployeeId(e.target.value)}
            required
          >
            <option value="">선택</option>
            {employees.map((employee) => (
              <option key={employee.employeeId} value={employee.employeeId}>
                {employee.employeeName}
              </option>
            ))}
          </select>
        </p>

        <p>
          승인자:
          <select
            value={approverId}
            onChange={(e) => setApproverId(e.target.value)}
            required
          >
            <option value="">선택</option>
            {employees.map((employee) => (
              <option key={employee.employeeId} value={employee.employeeId}>
                {employee.employeeName}
              </option>
            ))}
          </select>
        </p>

        <p>
          시작일:
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            required
          />
        </p>

        <p>
          종료일:
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            required
          />
        </p>

        <p>
          신청 사유:
          <input
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            maxLength={500}
            required
          />
        </p>

        <button type="submit" disabled={submitting}>
          {submitting ? "신청 중..." : "휴가 신청"}
        </button>
      </form>

      <p>{message}</p>

      <hr />

      {/* 휴가 검색 */}
      <h2>휴가 목록</h2>

      <form onSubmit={handleSearch}>
        <p>
          신청자:
          <select name="employeeId">
            <option value="">전체</option>
            {employees.map((employee) => (
              <option key={employee.employeeId} value={employee.employeeId}>
                {employee.employeeName}
              </option>
            ))}
          </select>
        </p>

        <p>
          승인자:
          <select name="approverId">
            <option value="">전체</option>
            {employees.map((employee) => (
              <option key={employee.employeeId} value={employee.employeeId}>
                {employee.employeeName}
              </option>
            ))}
          </select>
        </p>

        <p>
          상태:
          <select name="status">
            <option value="">전체</option>
            <option value="PENDING">승인 대기</option>
            <option value="APPROVED">승인</option>
            <option value="REJECTED">반려</option>
            <option value="CANCELED">취소</option>
          </select>
        </p>

        <p>
          정렬:
          <select name="sort">
            <option value="newest">최신순</option>
            <option value="oldest">오래된 순</option>
          </select>
        </p>

        <p>
          페이지 크기:
          <select name="size">
            <option value="5">5건</option>
            <option value="10">10건</option>
            <option value="20">20건</option>
          </select>
        </p>

        <button type="submit">검색</button>
      </form>

      <hr />

      {/* 휴가 목록 결과 */}
      {loading ? (
        <p>조회 중...</p>
      ) : listError ? (
        <p>{listError}</p>
      ) : (
        leavePage && (
          <>
            <p>전체 {leavePage.totalElements}건</p>

            <table>
              <thead>
                <tr>
                  <th>번호</th>
                  <th>신청자</th>
                  <th>승인자</th>
                  <th>시작일</th>
                  <th>종료일</th>
                  <th>상태</th>
                  <th>상세</th>
                </tr>
              </thead>

              <tbody>
                {leavePage.content.map((leave) => (
                  <tr key={leave.requestId}>
                    <td>{leave.requestId}</td>
                    <td>{leave.applicantName}</td>
                    <td>{leave.approverName}</td>
                    <td>{leave.startDate}</td>
                    <td>{leave.endDate}</td>
                    <td>{leave.status}</td>
                    <td>
                      <button
                        type="button"
                        onClick={() => {
                          setComment("");
                          void openDetail(leave.requestId);
                        }}
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {leavePage.content.length === 0 && <p>조회된 휴가가 없습니다.</p>}

            <p>
              <button
                type="button"
                disabled={page <= 1}
                onClick={() => {
                  setPage(page - 1);
                  setDetail(null);
                }}
              >
                이전
              </button>{" "}
              {leavePage.page} / {leavePage.totalPages}{" "}
              <button
                type="button"
                disabled={page >= leavePage.totalPages}
                onClick={() => {
                  setPage(page + 1);
                  setDetail(null);
                }}
              >
                다음
              </button>
            </p>
          </>
        )
      )}

      <hr />

      {/* 신규: 상세 및 처리 이력 */}
      <h2>휴가 상세</h2>

      {detailLoading && <p>상세 조회 중...</p>}
      {detailError && <p>{detailError}</p>}

      {detail && (
        <div>
          <p>신청 번호: {detail.requestId}</p>
          <p>신청자: {detail.applicantName}</p>
          <p>승인자: {detail.approverName}</p>
          <p>
            휴가 기간: {detail.startDate} ~ {detail.endDate}
          </p>
          <p>사유: {detail.reason}</p>
          <p>상태: {detail.status}</p>

          {detail.rejectionComment && (
            <p>반려 사유: {detail.rejectionComment}</p>
          )}

          {/* PENDING 상태만 처리 가능 */}
          {detail.status === "PENDING" && (
            <div>
              <h3>휴가 처리</h3>
              <button
                type="button"
                disabled={acting}
                onClick={() => handleAction("approve")}
              >
                승인
              </button>
              <p>
                반려 사유:
                <input
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  maxLength={500}
                  disabled={acting}
                />
              </p>
              <button
                type="button"
                disabled={acting}
                onClick={() => handleAction("reject")}
              >
                반려
              </button>{" "}
              <button
                type="button"
                disabled={acting}
                onClick={() => handleAction("cancel")}
              >
                취소
              </button>
            </div>
          )}

          <p>{actionMessage}</p>

          <h3>처리 이력</h3>

          {histories.length === 0 ? (
            <p>처리 이력이 없습니다.</p>
          ) : (
            <ul>
              {histories.map((history) => (
                <li key={history.historyId}>
                  {history.action} / {history.actorName}
                  {" / "}
                  {history.actedAt}
                  {history.comment && ` / ${history.comment}`}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

export default App;
