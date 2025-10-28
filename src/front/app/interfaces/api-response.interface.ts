export interface APIResponse<T> {
    method: "GET" | "HEAD" | "PUT" | "DELETE" | "POST";
    path: string;
    status: number;
    isOk: boolean;
    data: T|null;
}
