import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../store/auth";

const navTabs = [
  { to: "/problems", label: "Problemset" },
  { to: "/contests", label: "Contests" },
  { to: "/status", label: "Status" },
  { to: "/ratings", label: "Rating" },
  { to: "/blogs", label: "Blog" },
];

export function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/problems");
  }

  return (
    <header className="border-b-2 border-cf-blue">
      {/* Top band: logo + auth */}
      <div className="bg-white">
        <div className="mx-auto flex max-w-[1000px] items-center justify-between px-4 py-3">
          <Link to="/problems" className="flex items-baseline gap-1 no-underline">
            <span className="text-[28px] font-bold tracking-tight text-cf-blue">
              Code<span className="text-cf-red">Arena</span>
            </span>
          </Link>
          <div className="text-[12px]">
            {user ? (
              <span className="flex items-center gap-3">
                <Link to={`/users/${user.username}`} className="font-bold">
                  {user.username}
                </Link>
                <button onClick={handleLogout} className="text-cf-blue hover:underline">
                  Logout
                </button>
              </span>
            ) : (
              <span className="flex items-center gap-3">
                <Link to="/login">Enter</Link>
                <Link to="/register" className="font-bold">
                  Register
                </Link>
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Nav tabs band */}
      <nav className="bg-cf-blue">
        <div className="mx-auto flex max-w-[1000px] gap-0 px-2">
          {navTabs.map((t) => (
            <NavLink
              key={t.to}
              to={t.to}
              className={({ isActive }) =>
                `px-4 py-2 text-[13px] font-bold no-underline hover:bg-white/15 ${
                  isActive ? "bg-white text-cf-blue" : "text-white"
                }`
              }
            >
              {t.label}
            </NavLink>
          ))}
        </div>
      </nav>
    </header>
  );
}
