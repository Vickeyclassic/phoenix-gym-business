import {
  useState,
  useRef,
  useLayoutEffect,
  useCallback,
  useEffect,
} from "react";
import { api, message } from "../api";
export function useLoad(path, initial = []) {
  const [data, setData] = useState(initial),
    [resolvedPath, setResolvedPath] = useState(""),
    [loading, setLoading] = useState(true),
    [error, setError] = useState("");
  const request = useRef(0),
    currentPath = useRef(path);
  useLayoutEffect(() => {
    currentPath.current = path;
  }, [path]);
  const load = useCallback(async () => {
    const id = ++request.current;
    setLoading(true);
    setError("");
    try {
      const response = await api.get(path);
      if (id === request.current && currentPath.current === path) {
        setData(response.data);
        setResolvedPath(path);
      }
    } catch (e) {
      if (id === request.current && currentPath.current === path)
        setError(message(e));
    } finally {
      if (id === request.current && currentPath.current === path)
        setLoading(false);
    }
  }, [path]);
  useEffect(() => {
    load();
    return () => {
      request.current++;
    };
  }, [load]);
  return {
    data,
    resolvedPath,
    loading,
    error,
    load,
  };
}
