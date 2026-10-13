import { useRef, useState, useEffect } from "react";
import { squareCropFrame, validateLogoFile, logoSource } from "../branding";
import { api, message } from "../api";
import { Dumbbell } from "lucide-react";
import { Field, Modal, Notice, State } from "../components/ui.jsx";
import { useLoad } from "../hooks/useLoad.js";
export function LogoCropper({
  file,
  onCancel,
  onApply,
  subject = "business logo",
}) {
  const canvas = useRef(null),
    [image, setImage] = useState(null),
    [zoom, setZoom] = useState(1),
    [offsetX, setOffsetX] = useState(0),
    [offsetY, setOffsetY] = useState(0),
    [error, setError] = useState("");
  useEffect(() => {
    const url = URL.createObjectURL(file),
      img = new Image();
    img.onload = () => {
      setImage(img);
      setError("");
    };
    img.onerror = () => setError("Unable to read this image.");
    img.src = url;
    return () => {
      img.onload = null;
      img.onerror = null;
      URL.revokeObjectURL(url);
    };
  }, [file]);
  useEffect(() => {
    if (!image || !canvas.current) return;
    const context = canvas.current.getContext("2d"),
      frame = squareCropFrame(
        image.naturalWidth,
        image.naturalHeight,
        zoom,
        offsetX / 100,
        offsetY / 100,
      );
    context.clearRect(0, 0, 512, 512);
    context.drawImage(
      image,
      frame.x,
      frame.y,
      frame.size,
      frame.size,
      0,
      0,
      512,
      512,
    );
  }, [image, zoom, offsetX, offsetY]);
  function apply() {
    canvas.current.toBlob((blob) => {
      if (!blob) return setError("Unable to create the cropped image.");
      const cropped = new File([blob], "logo-cropped.png", {
        type: "image/png",
      });
      const invalid = validateLogoFile(cropped);
      if (invalid) return setError(invalid);
      onApply(cropped, canvas.current.toDataURL("image/png"));
    }, "image/png");
  }
  return (
    <Modal title={`Crop ${subject}`} onClose={onCancel}>
      <div className="logo-cropper">
        {error && <Notice error text={error} />}
        <canvas
          ref={canvas}
          width="512"
          height="512"
          aria-label={`Square ${subject} crop preview`}
        />
        <label>
          Zoom{" "}
          <input
            type="range"
            min="1"
            max="3"
            step="0.05"
            value={zoom}
            onChange={(e) => setZoom(Number(e.target.value))}
          />
        </label>
        <label>
          Horizontal position{" "}
          <input
            type="range"
            min="-100"
            max="100"
            value={offsetX}
            onChange={(e) => setOffsetX(Number(e.target.value))}
          />
        </label>
        <label>
          Vertical position{" "}
          <input
            type="range"
            min="-100"
            max="100"
            value={offsetY}
            onChange={(e) => setOffsetY(Number(e.target.value))}
          />
        </label>
        <small>
          The saved image is cropped square and resized to 512 × 512 PNG.
        </small>
        <div className="form-actions">
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
          <button
            type="button"
            className="primary"
            disabled={!image}
            onClick={apply}
          >
            Use cropped {subject}
          </button>
        </div>
      </div>
    </Modal>
  );
}
export function BusinessSettings({ user, notify }) {
  const x = useLoad("/settings", {}),
    [f, setF] = useState({}),
    [logoFile, setLogoFile] = useState(null),
    [cropFile, setCropFile] = useState(null),
    [preview, setPreview] = useState(""),
    [logoError, setLogoError] = useState(""),
    [removeLogo, setRemoveLogo] = useState(false),
    [busy, setBusy] = useState(false);
  useEffect(() => setF(x.data || {}), [x.data]);
  useEffect(
    () => () => {
      if (preview.startsWith("blob:")) URL.revokeObjectURL(preview);
    },
    [preview],
  );
  function chooseLogo(file) {
    if (!file) return;
    const error = validateLogoFile(file);
    setLogoError(error);
    if (!error) setCropFile(file);
  }
  function applyCrop(file, dataUrl) {
    setCropFile(null);
    setLogoFile(file);
    setPreview(dataUrl);
    setRemoveLogo(false);
    setLogoError("");
  }
  function clearLogo() {
    setCropFile(null);
    setLogoFile(null);
    setPreview("");
    setLogoError("");
    setRemoveLogo(true);
  }
  async function save(e) {
    e.preventDefault();
    if (logoError) return;
    setBusy(true);
    try {
      let next = {
        ...f,
      };
      if (logoFile) {
        const body = new FormData();
        body.append("file", logoFile);
        next.logoUrl = (await api.post("/settings/logo", body)).data.logoUrl;
      } else if (removeLogo) next.logoUrl = "";
      const saved = (await api.put("/settings", next)).data;
      setF(saved);
      setLogoFile(null);
      setPreview("");
      setRemoveLogo(false);
      window.dispatchEvent(new Event("business-branding"));
      notify("Business settings saved");
    } catch (err) {
      notify(message(err), "error");
    } finally {
      setBusy(false);
    }
  }
  const previewSource =
    preview || (removeLogo ? "" : logoSource(f.logoUrl, api.defaults.baseURL));
  return (
    <State {...x}>
      <form className="settings" onSubmit={save}>
        <div className="logo-settings">
          <div className="logo-preview">
            {previewSource ? (
              <img src={previewSource} alt="Business logo preview" />
            ) : (
              <Dumbbell />
            )}
            <div>
              <strong>{f.gymName || "Business logo"}</strong>
              <span>
                {removeLogo
                  ? "Logo will be removed when saved"
                  : logoFile
                    ? "Cropped logo ready to save"
                    : f.logoUrl
                      ? "Current logo"
                      : "No logo selected"}
              </span>
            </div>
          </div>
          {user.role !== "STAFF" && (
            <div className="logo-actions">
              <label className="file-button">
                Choose and crop logo
                <input
                  type="file"
                  accept="image/png,image/jpeg"
                  onChange={(e) => {
                    chooseLogo(e.target.files?.[0]);
                    e.target.value = "";
                  }}
                />
              </label>
              {(previewSource || f.logoUrl) && (
                <button type="button" onClick={clearLogo}>
                  Remove logo
                </button>
              )}
              <small>
                PNG or JPEG, up to 2 MB. Output: square 512 × 512 PNG.
              </small>
              {logoError && <span className="field-error">{logoError}</span>}
            </div>
          )}
        </div>
        {[
          ["gymName", "Gym name"],
          ["phone", "Business phone"],
          ["email", "Business email"],
          ["address", "Address"],
          ["currency", "Currency"],
          ["timeZone", "Time zone — IST / UTC+05:30"],
          ["taxIdentifier", "GST / Tax ID"],
          ["receiptFooter", "Receipt footer"],
          ["reminderDays", "Reminder days"],
        ].map(([k, l]) => (
          <Field
            key={k}
            label={l}
            value={f[k]}
            disabled={user.role === "STAFF" || k === "timeZone"}
            onChange={(v) =>
              setF({
                ...f,
                [k]: k === "reminderDays" ? Number(v) : v,
              })
            }
          />
        ))}
        {user.role !== "STAFF" && (
          <button className="primary" disabled={busy || Boolean(logoError)}>
            {busy ? "Saving…" : "Save settings"}
          </button>
        )}
      </form>
      {cropFile && (
        <LogoCropper
          file={cropFile}
          onCancel={() => setCropFile(null)}
          onApply={applyCrop}
        />
      )}
    </State>
  );
}
