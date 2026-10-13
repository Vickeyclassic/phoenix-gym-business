import { createContext, useContext, useState } from "react";
import { logoSource } from "../branding";
import { api } from "../api";
import { Dumbbell } from "lucide-react";
export const BrandingContext = createContext({
  gymName: "Phoenix Business",
  logoUrl: null,
});
export function Brand({ branding }) {
  const current = useContext(BrandingContext),
    [failed, setFailed] = useState("");
  const b = branding || current;
  const src = logoSource(b.logoUrl, api.defaults.baseURL);
  return (
    <>
      {src && failed !== src ? (
        <img
          className="business-logo"
          src={src}
          alt=""
          onError={() => setFailed(src)}
        />
      ) : (
        <Dumbbell />
      )}
      <span>{b.gymName || "Phoenix Business"}</span>
    </>
  );
}
export function BusinessIdentity() {
  const b = useContext(BrandingContext);
  return <p>{b.gymName} · Gym operations</p>;
}
