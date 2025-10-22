export interface APIResponse<T> {
    method: "GET" | "HEAD" | "PUT" | "DELETE" | "POST";
    path: string;
    status: number;
    data: T;
}
